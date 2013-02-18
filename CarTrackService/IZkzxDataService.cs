using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.ServiceModel.Web;
using System.Text;

namespace CarTrackService
{
    [DataContract]
    public class StringValue
    {
        [DataMember]
        public string Value
        {
            get;
            set;
        }
    }

    [ServiceContract]
    interface IZkzxDataService : IGpsDataService
    {
        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetAppVersionCode")]
        StringValue GetAppVersionCode();

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetWorkSequence/{sWorkerId}")]
        StringValue GetWorkSequence(string sWorkerId);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetWorkDetails/{sWorkerId}/{sActionIdx}")]
        StringValue GetWorkDetails(string sWorkerId, string sActionIdx);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetCurrentWorkerIdByTruckId/{sTruckId}")]
        StringValue GetCurrentWorkerIdByTruckId(string sTruckId);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/GetWorkerIdByTruckId/{sTruckId}/{sIdx}")]
        StringValue GetWorkerIdByTruckId(string sTruckId, string sIdx);

        [OperationContract]
        [WebInvoke(Method = "GET", ResponseFormat = WebMessageFormat.Json,
            BodyStyle = WebMessageBodyStyle.Bare, UriTemplate = "/SendTruckState/{sWorkerId}/{state}")]
        StringValue SendTruckState(string sWorkerId, string state);
    }
}
