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
        
//        static const std::string UPDATE_INFO_PREFIX = "/org/openmhealth/haitao/data/fitness/physical_activity/time_location/update_info";
//        static const std::string CATALOG_PREFIX = "/org/openmhealth/haitao/data/fitness/physical_activity/time_location/catalog";
//        static const std::string DATA_PREFIX = "/org/openmhealth/haitao/data/fitness/physical_activity/time_location";
//        
//        static const std::string UPDATE_INFO_CONFIRM_PREFIX = "/edu/ucla/remap/dsu/org/openmhealth/haitao/data/fitness/physical_activity/time_location/update_info";
//        static const std::string CATALOG_CONFIRM_PREFIX = "/edu/ucla/remap/dsu/org/openmhealth/haitao/data/fitness/physical_activity/time_location/catalog";
//        static const std::string DATA_CONFIRM_PREFIX = "/edu/ucla/remap/dsu/org/openmhealth/haitao/data/fitness/physical_activity/time_location";
        
        static const std::string COMMON_PREFIX = "/org/openmhealth";
        static const std::string UPDATE_INFO_SUFFIX = "/data/fitness/physical_activity/time_location/update_info";
        static const std::string CATALOG_SUFFIX = "/data/fitness/physical_activity/time_location/catalog";
        static const std::string DATA_SUFFIX = "/data/fitness/physical_activity/time_location";

        
        static const std::string CONFIRM_PREFIX = "/ndn/edu/ucla/remap/ndnfit/dsu/confirm/org/openmhealth";
        static const std::string REGISTER_PREFIX = "/ndn/edu/ucla/remap/ndnfit/dsu/register/org/openmhealth";
        
        class DSUsync : noncopyable
        {
        public:
            DSUsync()
            : m_face(m_ioService) // Create face with io_service object
            //, tcp_connect_repo("localhost", "7376")
            , m_scheduler(m_ioService)
            {
                //tcp_connect_repo.connect(m_ioService, DSUsync::receiveCallback);
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
            static void receiveCallback(const Block& wire) {
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
                unretrieveMap.erase(data.getName());
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
                    catalogInterest.setInterestLifetime(time::seconds(60));
                    catalogInterest.setMustBeFresh(true);
                    m_face.expressInterest(catalogInterest,
                                           bind(&DSUsync::onCatalogData, this, _1, _2),
                                           bind(&DSUsync::onCatalogTimeout, this, _1));
                    std::cout << "Sending " << catalogInterest << std::endl;
                    unretrieveMap.insert(std::pair<Name, int>(catalogInterest.getName(), 0));
                }
                
                // continue to fetch the next update information packet
                uint64_t seqNo = interest.getName().get(-1).toSequenceNumber();
                Interest updateInfoInterest(interest.getName().getPrefix(-1).appendSequenceNumber(seqNo + 1));
                updateInfoInterest.setInterestLifetime(time::seconds(60));
                updateInfoInterest.setMustBeFresh(true);
                m_face.expressInterest(updateInfoInterest,
                                       bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                       bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                std::cout << "Sending " << updateInfoInterest << std::endl;
                unretrieveMap.insert(std::pair<Name, int>(updateInfoInterest.getName(), 0));
            }
            
            void onUpdateInfoTimeout (const Interest& interest)
            {
                std::map<Name, int>::iterator it;
                it = unretrieveMap.find(interest.getName());
                if(it == unretrieveMap.end()) {
                    std::cout << "I didn't try to retrieve " << interest << std::endl;
                    return;
                }
                int updateInfoRetry = it->second;
                if(updateInfoRetry == INT_MAX) {
                    std::cout << "Timeout " << interest << std::endl;
                    updateInfoRetry = 0;
                } else {
                    Interest updateInfoInterest(interest.getName());
                    updateInfoInterest.setInterestLifetime(time::seconds(60));
                    updateInfoInterest.setMustBeFresh(true);
                    m_face.expressInterest(updateInfoInterest,
                                           bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                           bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                    std::cout << "Sending " << updateInfoInterest << std::endl;
                    updateInfoRetry++;
                }
                it->second = updateInfoRetry;
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
                unretrieveMap.erase(data.getName());
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
                    datapointInterest.setInterestLifetime(time::seconds(60));
                    datapointInterest.setMustBeFresh(true);
                    m_face.expressInterest(datapointInterest,
                                           bind(&DSUsync::onDatapointData, this, _1, _2),
                                           bind(&DSUsync::onDatapointTimeout, this, _1));
                    std::cout << "Sending " << datapointInterest << std::endl;
                    unretrieveMap.insert(std::pair<Name, int>(datapointInterest.getName(), 0));
                }
                
            }
            
            void onCatalogTimeout (const Interest& interest)
            {
                std::map<Name, int>::iterator it;
                it = unretrieveMap.find(interest.getName());
                if(it == unretrieveMap.end()) {
                    std::cout << "I didn't try to retrieve " << interest << std::endl;
                    return;
                }
                int catalogRetry = it->second;
                if(catalogRetry == 3) {
                    std::cout << "Timeout " << interest << std::endl;
                    catalogRetry = 0;
                } else {
                    Name previousName = interest.getName();
                    Interest catalogInterest(interest.getName());
                    catalogInterest.setInterestLifetime(time::seconds(60));
                    catalogInterest.setMustBeFresh(true);
                    m_face.expressInterest(catalogInterest,
                                           bind(&DSUsync::onCatalogData, this, _1, _2),
                                           bind(&DSUsync::onCatalogTimeout, this, _1));
                    std::cout << "Sending " << catalogInterest << std::endl;
                    catalogRetry++;
                }
                it->second = catalogRetry;
            }
            
            void onDatapointData(const Interest& interest, const Data& data)
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
                unretrieveMap.erase(data.getName());
                confirmSet.insert(data.getName());
                
                //put data into repo
                //tcp_connect_repo.send(data.wireEncode());
                
            }
            
            void onDatapointTimeout (const Interest& interest)
            {
                std::map<Name, int>::iterator it;
                it = unretrieveMap.find(interest.getName());
                if(it == unretrieveMap.end()) {
                    std::cout << "I didn't try to retrieve " << interest << std::endl;
                    return;
                }
                int datapointRetry = it->second;
                if(datapointRetry == 3) {
                    std::cout << "Timeout " << interest << std::endl;
                    datapointRetry = 0;
                } else {
                    Name previousName = interest.getName();
                    Interest datapointInterest(interest.getName());
                    datapointInterest.setInterestLifetime(time::seconds(60));
                    datapointInterest.setMustBeFresh(true);
                    m_face.expressInterest(datapointInterest,
                                           bind(&DSUsync::onDatapointData, this, _1, _2),
                                           bind(&DSUsync::onDatapointTimeout, this, _1));
                    std::cout << "Sending " << datapointInterest << std::endl;
                    datapointRetry++;
                }
                it->second = datapointRetry;
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
                }
            }
            
            void
            onRegisterInterest(const InterestFilter& filter, const Interest& interest)
            {
                std::cout << "<< I: " << interest << std::endl;
                
                name::Component user_id = interest.getName().get(9);
                //send out update information interest
                Interest updateInfoInterest(Name(COMMON_PREFIX).append(user_id).append(Name(UPDATE_INFO_SUFFIX)).appendSequenceNumber(1));
                updateInfoInterest.setInterestLifetime(time::seconds(60));
                updateInfoInterest.setMustBeFresh(true);
                m_face.expressInterest(updateInfoInterest,
                                       bind(&DSUsync::onUpdateInfoData, this, _1, _2),
                                       bind(&DSUsync::onUpdateInfoTimeout, this, _1));
                std::cout << "Sending " << updateInfoInterest << std::endl;
                unretrieveMap.insert(std::pair<Name, int>(updateInfoInterest.getName(), 0));
                //unretrieveMap[updateInfoInterest.getName()] = 0; this is the easy way to insert data
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
            //TcpTransport tcp_connect_repo;
            Scheduler m_scheduler;
            std::map<Name, int> unretrieveMap;
            std::set<Name> confirmSet;
            KeyChain m_keyChain;
        };
        
        
        
    } // namespace examples
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
