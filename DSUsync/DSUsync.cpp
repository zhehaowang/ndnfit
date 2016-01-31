#include <string>
#include <ndn-cxx/security/key-chain.hpp>
#include <ndn-cxx/face.hpp>
#include <ndn-cxx/util/scheduler.hpp>
#include <ndn-cxx/util/time.hpp>
#include <ndn-cxx/transport/tcp-transport.hpp>
#include <rapidjson/document.h>		// rapidjson's DOM-style API
#include <rapidjson/prettywriter.h>	// for stringify JSON
#include <rapidjson/filestream.h>	// wrapper of C stream for prettywriter as output
#include <cstdio>

namespace ndn {
    namespace dsu {
        
        static const std::string COMMON_PREFIX = "/org/openmhealth";
        static const std::string UPDATE_INFO_SUFFIX = "/data/fitness/physical_activity/time_location/update_info";
        static const std::string CATALOG_SUFFIX = "/data/fitness/physical_activity/time_location/catalog";
        static const std::string DATA_SUFFIX = "/data/fitness/physical_activity/time_location";
        
        static const name::Component CATALOG_COMP("catalog");
        static const name::Component UPDATA_INFO_COMP("update_info");
        
        static const int INTEREST_TIME_OUT_SECONDS = 60;
        
        static const std::string CONFIRM_PREFIX = "/ndn/edu/ucla/remap/ndnfit/dsu/confirm/org/openmhealth";
        static const std::string REGISTER_PREFIX = "/ndn/edu/ucla/remap/ndnfit/dsu/register/org/openmhealth";
        
        class DSUsync : noncopyable
        {
        public:
            DSUsync()
            : m_face(m_ioService) // Create face with io_service object
            , tcp_connect_repo("localhost", "6363" /*7376*/)
            , m_scheduler(m_ioService)
            {
                tcp_connect_repo.connect(m_ioService, bind(&DSUsync::receiveCallback, this, _1));
            }
            
            void
            run()
            {
                
                //accept incoming confirm interest
                m_face.setInterestFilter(CONFIRM_PREFIX,
                                         bind(&DSUsync::onConfirmInterest, this, _1, _2),
                                         RegisterPrefixSuccessCallback(),
                                         bind(&DSUsync::onRegisterFailed, this, _1, _2));
                
                //accept incoming register interest
                m_face.setInterestFilter(REGISTER_PREFIX,
                                         bind(&DSUsync::onRegisterInterest, this, _1, _2),
                                         RegisterPrefixSuccessCallback(),
                                         bind(&DSUsync::onRegisterFailed, this, _1, _2));
                
                // m_ioService.run() will block until all events finished or m_ioService.stop() is called
                m_ioService.run();
                
                // Alternatively, m_face.processEvents() can also be called.
                // processEvents will block until the requested data received or timeout occurs.
                // m_face.processEvents();
            }
            
        private:
            void receiveCallback(const Block& wire) {
                return;
            }
            void onUpdateInfoData(const Interest& interest, const Data& data)
            {
                std::string content((char *)data.getContent().value(), data.getContent().value_size());
                std::cout << content << std::endl;
                char buffer[data.getContent().value_size()+1];
                std::strcpy(buffer, content.c_str());
                rapidjson::Document document;
                if (document.ParseInsitu<0>(buffer).HasParseError())
                {
                    std::cout << "Parsing " << data << " error!" << std::endl;
                }
                
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it != outer_it->second.end()) {
                        outer_it->second.erase(inner_it);
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                confirmSet.insert(data.getName());
                //put data into repo
                //tcp_connect_repo.send(data.wireEncode());
                
                //parse the content and start to fetch catalog packets, see schema file for the details
                const rapidjson::Value& list = document;
                assert(list.IsArray());
                for (rapidjson::SizeType i = 0; i<list.Size(); i++) {
                    assert(list[i].IsObject());
                    assert(list[i]["timepoint"].IsNumber());
                    assert(list[i]["timepoint"].IsUInt64());
                    assert(list[i]["version"].IsNumber());
                    assert(list[i]["version"].IsUInt64());
                    
                    //send out catalog interest
                    Interest catalogInterest(interest.getName().getPrefix(-2).append("catalog")
                                             .appendTimestamp(time::fromUnixTimestamp(time::milliseconds(list[i]["timepoint"].GetUint64() / 1000)))
                                             .appendVersion(list[i]["version"].GetUint64()));
                    catalogInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    catalogInterest.setMustBeFresh(true);
                    m_face.expressInterest(catalogInterest,
                                           bind(&DSUsync::onCatalogData, this, _1, _2),
                                           bind(&DSUsync::onCatalogTimeout, this, _1));
                    std::cout << "Sending " << catalogInterest << std::endl;
//                    unretrieveMap.insert(std::pair<Name, int>(catalogInterest.getName(), 0));
                    outer_it->second[catalogInterest.getName()] = 0;
                }
                
                // continue to fetch the next update information packet
                uint64_t seqNo = interest.getName().get(-1).toSequenceNumber();
                Interest updateInfoInterest(interest.getName().getPrefix(-1).appendSequenceNumber(seqNo + 1));
                updateInfoInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                updateInfoInterest.setMustBeFresh(true);
                m_face.expressInterest(updateInfoInterest,
                                       bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                       bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                std::cout << "Sending " << updateInfoInterest << std::endl;
//                unretrieveMap.insert(std::pair<Name, int>(updateInfoInterest.getName(), 0));
                outer_it->second[updateInfoInterest.getName()] = 0;
            }
            
            void onUpdateInfoTimeout (const Interest& interest)
            {
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it == outer_it->second.end()) {
                        std::cout << "I didn't try to retrieve " << interest.getName().toUri()<< std::endl;
                        return;
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                
                int updateInfoRetry = inner_it->second;
                if(updateInfoRetry == INT_MAX) {
                    std::cout << "Timeout " << interest << std::endl;
                    updateInfoRetry = 0;
                } else {
                    Interest updateInfoInterest(interest.getName());
                    updateInfoInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    updateInfoInterest.setMustBeFresh(true);
                    m_face.expressInterest(updateInfoInterest,
                                           bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                           bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                    std::cout << "Sending " << updateInfoInterest << std::endl;
                    updateInfoRetry++;
                }
                inner_it->second = updateInfoRetry;
            }
            
            void onCatalogData(const Interest& interest, const Data& data)
            {
                std::string content((char *)data.getContent().value(), data.getContent().value_size());
                std::cout << content << std::endl;
                char buffer[data.getContent().value_size()+1];
                std::strcpy(buffer, content.c_str());
                rapidjson::Document document;
                if (document.ParseInsitu<0>(buffer).HasParseError())
                {
                    std::cout << "Parsing " << data << " error!" << std::endl;
                }
                
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it != outer_it->second.end()) {
                        outer_it->second.erase(inner_it);
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                confirmSet.insert(data.getName());
                //put data into repo
                //tcp_connect_repo.send(data.wireEncode());
                
                //parse the content and start to fetch the data points, see schema file for the details
                const rapidjson::Value& list = document;
                assert(list.IsArray());
                for (rapidjson::SizeType i = 0; i<list.Size(); i++) {
                    assert(list[i].IsNumber());
                    assert(list[i].IsUInt64());
                    
                    //std::cout << list[i].GetUint64() << std::endl;
                    // send out datapoints interest
                    Interest datapointInterest(interest.getName().getPrefix(-3).appendTimestamp(time::fromUnixTimestamp(time::milliseconds(list[i].GetUint64()/1000))));
                    datapointInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    datapointInterest.setMustBeFresh(true);
                    m_face.expressInterest(datapointInterest,
                                           bind(&DSUsync::onDatapointData, this, _1, _2),
                                           bind(&DSUsync::onDatapointTimeout, this, _1));
                    std::cout << "Sending " << datapointInterest << std::endl;
//                    unretrieveMap.insert(std::pair<Name, int>(datapointInterest.getName(), 0));
                    outer_it->second[datapointInterest.getName()] = 0;
                }
                
            }
            
            void onCatalogTimeout (const Interest& interest)
            {
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it == outer_it->second.end()) {
                        std::cout << "I didn't try to retrieve " << interest << std::endl;
                        return;
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                int catalogRetry = inner_it->second;
                if(catalogRetry == 3) {
                    std::cout << "Timeout " << interest << std::endl;
                    catalogRetry = 0;
                } else {
                    Name previousName = interest.getName();
                    Interest catalogInterest(interest.getName());
                    catalogInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    catalogInterest.setMustBeFresh(true);
                    m_face.expressInterest(catalogInterest,
                                           bind(&DSUsync::onCatalogData, this, _1, _2),
                                           bind(&DSUsync::onCatalogTimeout, this, _1));
                    std::cout << "Sending " << catalogInterest << std::endl;
                    catalogRetry++;
                }
                inner_it->second = catalogRetry;
            }
            
            void onDatapointData(const Interest& interest, const Data& data)
            {
                std::string content((char *)data.getContent().value(), data.getContent().value_size());
                std::cout << content << std::endl;
                // to correctly parse the JSON, the '\0' should be included.
                char buffer[data.getContent().value_size()+1];
                std::strcpy(buffer, content.c_str());
                rapidjson::Document document;
                if (document.ParseInsitu<0>(buffer).HasParseError())
                {
                    std::cout << "Parsing " << data << " error!" << std::endl;
                }
                
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it != outer_it->second.end()) {
                        outer_it->second.erase(inner_it);
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                confirmSet.insert(data.getName());
                
                //put data into repo
                //tcp_connect_repo.send(data.wireEncode());
                
            }
            
            void onDatapointTimeout (const Interest& interest)
            {
                name::Component user_id = interest.getName().get(2);
                
                std::map<name::Component, std::map<Name, int>>::iterator outer_it;
                outer_it = user_unretrieve_map.find(user_id);
                std::map<Name, int>::iterator inner_it;
                if (outer_it != user_unretrieve_map.end()) {
                    inner_it = outer_it->second.find(interest.getName());
                    if (inner_it == outer_it->second.end()) {
                        std::cout << "I didn't try to retrieve " << interest << std::endl;
                        return;
                    }
                } else {
                    //figure out what to do here
                    return;
                }
                
                int datapointRetry = inner_it->second;
                if(datapointRetry == 3) {
                    std::cout << "Timeout " << interest << std::endl;
                    datapointRetry = 0;
                } else {
                    Name previousName = interest.getName();
                    Interest datapointInterest(interest.getName());
                    datapointInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    datapointInterest.setMustBeFresh(true);
                    m_face.expressInterest(datapointInterest,
                                           bind(&DSUsync::onDatapointData, this, _1, _2),
                                           bind(&DSUsync::onDatapointTimeout, this, _1));
                    std::cout << "Sending " << datapointInterest << std::endl;
                    datapointRetry++;
                }
                inner_it->second = datapointRetry;
            }
            
            void
            onConfirmInterest(const InterestFilter& filter, const Interest& interest)
            {
                std::cout << "<< I: " << interest << std::endl;
                
                // Create new name, based on Interest's name
                Name confirmDataName(interest.getName());
                
                std::set<Name>::iterator it;
                it = confirmSet.find(confirmDataName.getSubName(4));
                if (it != confirmSet.end()) {
                    Data data;
                    data.setName(confirmDataName);
                    confirmSet.erase(it);
                }
            }
            
            void
            onRegisterInterest(const InterestFilter& filter, const Interest& interest)
            {
                std::cout << "<< I: " << interest << std::endl;
                
                name::Component user_id = interest.getName().get(9);
                
                std::map<name::Component, std::map<Name, int>>::iterator it;
                it = user_unretrieve_map.find(user_id);
                if (it != user_unretrieve_map.end()) {
                    for (std::map<Name,int>::iterator inner_it=it->second.begin(); inner_it!=it->second.end(); ++inner_it) {
                        name::Component keyComp = inner_it->first.get(7);
                        Interest resentInterest(inner_it->first);
                        resentInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                        resentInterest.setMustBeFresh(true);
                        if (keyComp == UPDATA_INFO_COMP) {
                            // TODO: the same update information interest may be sent, but this is not a big deal(at least for not)
                            m_face.expressInterest(resentInterest,
                                                   bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                                   bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                            
                        } else if(keyComp == CATALOG_COMP) {
                            m_face.expressInterest(resentInterest,
                                                   bind(&DSUsync::onCatalogData, this, _1, _2),
                                                   bind(&DSUsync::onCatalogTimeout, this, _1));
                        } else {
                            m_face.expressInterest(resentInterest,
                                                   bind(&DSUsync::onDatapointData, this, _1, _2),
                                                   bind(&DSUsync::onDatapointTimeout, this, _1));
                        }
                        std::cout << "Sending " << resentInterest << std::endl;
                    }
                } else {
                    //send out update information interest
                    Interest updateInfoInterest(Name(COMMON_PREFIX).append(user_id).append(Name(UPDATE_INFO_SUFFIX)).appendSequenceNumber(1));
                    updateInfoInterest.setInterestLifetime(time::seconds(INTEREST_TIME_OUT_SECONDS));
                    updateInfoInterest.setMustBeFresh(true);
                    m_face.expressInterest(updateInfoInterest,
                                           bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                           bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                    std::cout << "Sending " << updateInfoInterest << std::endl;
                    
                    std::map<Name, int> unretrieve_map;
                    user_unretrieve_map[user_id] = unretrieve_map;
                    unretrieve_map[updateInfoInterest.getName()] = 0;
                }
            }
            
            void
            onRegisterFailed(const Name& prefix, const std::string& reason)
            {
                std::cerr << "ERROR: Failed to register prefix \""
                << prefix << "\" in local hub's daemon (" << reason << ")"
                << std::endl;
            }
            
        private:
            // Explicitly create io_service object, which can be shared between Face and Scheduler
            boost::asio::io_service m_ioService;
            Face m_face;
            TcpTransport tcp_connect_repo;
            Scheduler m_scheduler;
            std::map<name::Component, std::map<Name, int>> user_unretrieve_map;
            std::set<Name> confirmSet;
            KeyChain m_keyChain;
        };
        
        
        
    } // namespace dsu
} // namespace ndn

int
main(int argc, char** argv)
{
    ndn::dsu::DSUsync dsusync;
    try {
        dsusync.run();
    }
    catch (const std::exception& e) {
        std::cerr << "ERROR: " << e.what() << std::endl;
    }
    return 0;
}
