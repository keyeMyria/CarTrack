using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.Text;
using Feng;

namespace CarTrackService
{
    // >aspnet_regiis -i -enable
    public class GpsDataService : IGpsDataService
    {
        private static Dictionary<long, IList<TrackPoint>> m_gpsData = new Dictionary<long, IList<TrackPoint>>();
        private static Dictionary<long, int> m_gpsDataIdx = new Dictionary<long, int>();

        private Offset m_offsetEmpty = new Offset { dLat = 0, dLon = 0 };
        public Offset GetOffset(string sKey)
        {
            //double lat = Convert.ToInt32(sLat) / 100;
            //double lon = Convert.ToInt32(sLon) / 100;
            //int zoom = Convert.ToInt32(sZoom);

            try
            {
                int key = Convert.ToInt32(sKey);
                var offset = Feng.Map.GoogleMapChinaOffset.Instance.GetOffset(key);
                return new Offset { dLat = offset.Y, dLon = offset.X };
            }
            catch (Exception)
            {
                return m_offsetEmpty;
            }
        }

        private IList<TrackPoint> ReloadTrackLocation(long trackId)
        {
            try
            {
                using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                {
                    Track track = rep.Get<Track>(trackId);
                    if (track != null)
                    {
                        var list = rep.List<TrackPoint>("from TrackPoint g where g.Track = :Track order by g.GpsTime asc",
                            new Dictionary<string, object> { { "Track", track } });

                        return list;
                    }
                }
            }
            catch (Exception)
            {
            }
            return null;
        }

        public Location GetLocation(string sTrackId)
        {
            if (string.IsNullOrEmpty(sTrackId))
                return null;

            long trackId = Convert.ToInt64(sTrackId);

            lock (m_gpsData)
            {
                if (!m_gpsData.ContainsKey(trackId))
                {
                    m_gpsData[trackId] = ReloadTrackLocation(trackId);
                    m_gpsDataIdx[trackId] = 0;
                }
            }

            if (m_gpsData.ContainsKey(trackId))
            {
                int idx = m_gpsDataIdx[trackId];

                TrackPoint gpsData = m_gpsData[trackId][idx];
                GMap.NET.PointLatLng p1 = new GMap.NET.PointLatLng { Lat = gpsData.Latitude, Lng = gpsData.Longitude };
                GMap.NET.PointLatLng p2;
                lock (Feng.Map.GoogleMapChinaOffset.Instance)
                {
                    p2 = Feng.Map.GoogleMapChinaOffset.Instance.GetOffseted(p1);
                }

                idx++;
                if (idx >= m_gpsData[trackId].Count)
                {
                    m_gpsData[trackId] = ReloadTrackLocation(trackId);
                }
                if (idx >= m_gpsData[trackId].Count)
                {
                    //idx = 0;
                    idx = m_gpsData[trackId].Count - 1;
                }

                m_gpsDataIdx[trackId] = idx;

                return new Location
                {
                    Latitude = p1.Lat,
                    Longitude = p1.Lng,
                    Heading = gpsData.Heading,
                    LatitudeChina = p2.Lat,
                    LongitudeChina = p2.Lng
                };
            }
            return null;
        }

        public Location GetLastLocation(string sTrackId)
        {
            if (string.IsNullOrEmpty(sTrackId))
                return null;

            long trackId = Convert.ToInt64(sTrackId);

            IList<TrackPoint> list = null;
            using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
            {
                Track track = rep.Get<Track>(trackId);
                if (track != null)
                {
                    list = rep.List<TrackPoint>("from TrackPoint g where g.Track = :Track order by g.GpsTime desc",
                        new Dictionary<string, object> { { "Track", track }, { "MaxResults", 1 } });
                }
            }
            if (list == null)
                return null;

            TrackPoint gpsData = list[0];
            GMap.NET.PointLatLng p1 = new GMap.NET.PointLatLng { Lat = gpsData.Latitude, Lng = gpsData.Longitude };
            GMap.NET.PointLatLng p2;
            lock (Feng.Map.GoogleMapChinaOffset.Instance)
            {
                p2 = Feng.Map.GoogleMapChinaOffset.Instance.GetOffseted(p1);
            }

            return new Location
            {
                Latitude = p1.Lat,
                Longitude = p1.Lng,
                Heading = gpsData.Heading,
                LatitudeChina = p2.Lat,
                LongitudeChina = p2.Lng
            };
        }

        #region IGpsDataService 成员


        GpsService m_soapService = new GpsService();
        public virtual string SendTrackPoint(string gpsId, string gpsData)
        {
            string[] ss = gpsData.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);
            if (ss.Length == 7)
            {
                Logger.Info(string.Format("SendTrackPoint: {0}, {1}", gpsId, gpsData));
                if (string.IsNullOrEmpty(gpsData))
                {
                    Logger.Warn("gpsData is null");
                    return string.Empty;
                }
                m_soapService.SendTrackPoint(gpsId, gpsData);
                return string.Empty;
            }
            else if (ss.Length == 8)
            {
                Logger.Info(string.Format("SendWayPoint: {0}, {1}", gpsId, gpsData));
                if (string.IsNullOrEmpty(gpsData))
                {
                    Logger.Warn("gpsData is null");
                    return string.Empty;
                }
                m_soapService.SendWayPoint(gpsId, gpsData);
                return string.Empty;
            }
            else
            {
                return string.Empty;
            }
        }

        //public string StartRecording(string gpsId, string startTime)
        //{
        //    Logger.Info("StartRecording");
        //    if (string.IsNullOrEmpty(startTime))
        //    {
        //        Logger.Warn("startTime is null");
        //        return string.Empty;
        //    }
        //    m_soapService.StartRecording(gpsId, Convert.ToDateTime(startTime));
        //    return string.Empty;
        //}

        //public string StopRecording(string gpsId, string endTime)
        //{
        //    Logger.Info("StopRecording");
        //    if (string.IsNullOrEmpty(endTime))
        //    {
        //        Logger.Warn("endTime is null");
        //        return string.Empty;
        //    }
        //    m_soapService.StopRecording(gpsId, Convert.ToDateTime(endTime));
        //    return string.Empty;
        //}

        private static TrackDao s_trackDao = new TrackDao();

        public string SendTrackData(string gpsId, string gpxData)
        {
            try
            {
                Feng.Track track = new Feng.Track();
                track.Name = "Gpx";
                track.VehicleName = gpsId;
                track.IsActive = true;
                track.Gpx = gpxData;
                track.StartTime = System.DateTime.Now;
                track.EndTime = System.DateTime.Now;
                s_trackDao.Save(track);
            }
            catch (Exception)
            {
                return string.Empty;
            }
            return string.Empty;
        }

        public string SendTrackInfo(string gpsId, string parameters)
        {
            if (string.IsNullOrEmpty(gpsId))
                return string.Empty;

            Newtonsoft.Json.Linq.JObject p = Newtonsoft.Json.Linq.JObject.Parse(parameters);
            if (p["Type"] == null)
                return string.Empty;
            switch (((string)p["Type"]).ToUpper())
            {
                //case "START":
                //    this.StartRecording(gpsId, (string)p["Time"]);
                //    break;
                //case "STOP":
                //    this.StopRecording(gpsId, (string)p["Time"]);
                //    break;
                case "TRACKPOINT":
                    this.SendTrackPoint(gpsId, (string)p["Data"]);
                    break;
                //case "WAYPOINT":
                //    this.SendWayPoint(gpsId, (string)p["Data"], (string)p["Action"]);
                //    break;
                case "GPX":
                    this.SendTrackData(gpsId, (string)p["Data"]);
                    break;
            }
            return string.Empty;
        }
        #endregion
    }
}
