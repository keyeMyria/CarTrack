using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;
using System.Text;

namespace CarTrackService
{
    /* Debug:
     * http://127.0.0.1/CarTrackService/GpsDataService.svc/ZJB-8092H/SendWayPoint
     * content-type: application/json
     * {"gpsData":"2010-01-01T12:12:12,1,1,1,1,1,1", "actionData":"aa"}
     * apk: http://192.168.0.10/CarTrackservice/MyTracks.apk
     */
    // 注意: 使用“重构”菜单上的“重命名”命令，可以同时更改代码和配置文件中的接口名“IGpsData”。
    [ServiceContract]
    public interface IGpsDataService
    {
        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetLocation/{sTrackId}")]
        Location GetLocation(string sTrackId);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetLastLocation/{sTrackId}")]
        Location GetLastLocation(string sTrackId);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetOffset?k={sKey}")]
        Offset GetOffset(string sKey);

        [OperationContract]
        [WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/SendTrackPoint")]
        string SendTrackPoint(string gpsId, string gpsData);

        //[OperationContract]
        //[WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
        //    BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/SendWayPoint")]
        //string SendWayPoint(string gpsId, string gpsData, string actionData);

        //[OperationContract]
        //[WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
        //    BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/StartRecording")]
        //string StartRecording(string gpsId, string startTime);

        //[OperationContract]
        //[WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
        //    BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/StopRecording")]
        //string StopRecording(string gpsId, string endTime);

        [OperationContract]
        [WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/SendTrackData")]
        string SendTrackData(string gpsId, string gpxData);

        //[OperationContract]
        //[WebInvoke(Method = "POST", RequestFormat = WebMessageFormat.Json, ResponseFormat = WebMessageFormat.Json,
        //    BodyStyle = WebMessageBodyStyle.WrappedRequest, UriTemplate = "/{gpsId}/SendTrackInfo")]
        //string SendTrackInfo(string gpsId, string parameters);
    }

    [DataContract]
    public class Offset
    {
        [DataMember]
        public int dLat
        {
            get;
            set;
        }
        [DataMember]
        public int dLon
        {
            get;
            set;
        }
    }
    [DataContract]
    public class Location
    {
        [DataMember]
        public double Latitude
        {
            get;
            set;
        }
        [DataMember]
        public double Longitude
        {
            get;
            set;
        }

        [DataMember]
        public double Heading
        {
            get;
            set;
        }

        [DataMember]
        public double LatitudeChina
        {
            get;
            set;
        }
        [DataMember]
        public double LongitudeChina
        {
            get;
            set;
        }
    }
}
