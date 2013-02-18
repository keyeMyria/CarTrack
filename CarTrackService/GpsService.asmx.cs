using System;
using System.Collections.Generic;
using System.Web;
using System.Web.Services;
using Feng;

namespace CarTrackService
{
    /// <summary>
    /// CarTrackWebService 的摘要说明
    /// </summary>
    [WebService(Namespace = "http://tempuri.org/")]
    [WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
    [System.ComponentModel.ToolboxItem(false)]
    // 若要允许使用 ASP.NET AJAX 从脚本中调用此 Web 服务，请取消对下行的注释。
    // [System.Web.Script.Services.ScriptService]
    public class GpsService : System.Web.Services.WebService
    {
        //internal static string ProcessVehicleName(string vehicleName)
        //{
        //    if (vehicleName.StartsWith("ZJ"))
        //    {
        //        vehicleName = vehicleName.Remove(0, 2).Insert(0, "浙");
        //    }
        //    return vehicleName;
        //}

        private WayPoint ConvertToGpsData2(string gpsId, string gpsData)
        {
            string[] ss = gpsData.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);

            WayPoint entity = new WayPoint
            {
                VehicleName = gpsId,
                GpsTime = Convert.ToDateTime(ss[0]),
                Latitude = Convert.ToDouble(ss[1]),
                Longitude = Convert.ToDouble(ss[2]),
                Accuracy = Convert.ToDouble(ss[3]),
                Altitude = Convert.ToDouble(ss[4]),
                Heading = Convert.ToDouble(ss[5]),
                Speed = Convert.ToDouble(ss[6]),
                MessageTime = System.DateTime.Now,
                IsActive = true,
                Action = ss[7]
            };
            return entity;
        }
        public static TrackPoint ConvertToGpsData(string gpsId, string gpsData)
        {
            string[] ss = gpsData.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries);

            TrackPoint entity = new TrackPoint
            {
                VehicleName = gpsId,
                GpsTime = Convert.ToDateTime(ss[0]),
                Latitude = Convert.ToDouble(ss[1]),
                Longitude = Convert.ToDouble(ss[2]),
                Accuracy = Convert.ToDouble(ss[3]),
                Altitude = Convert.ToDouble(ss[4]),
                Heading = Convert.ToDouble(ss[5]),
                Speed = Convert.ToDouble(ss[6]),
                MessageTime = System.DateTime.Now,
                IsActive = true
            };
            return entity;
        }

        private void SaveGpsData(Feng.IRepository rep, WayPoint entity)
        {
            if (rep == null)
            {
                s_wayPointDao.Save(entity);
            }
            else
            {
                s_wayPointDao.Save(rep, entity);
            }
        }

        private void SaveGpsData(Feng.IRepository rep, TrackPoint entity)
        {
            if (rep == null)
            {
                s_trackPointDao.Save(entity);
            }
            else
            {
                s_trackPointDao.Save(rep, entity);
            }
        }

        public bool SendWayPoint(string gpsId, string gpsData)
        {
            Logger.Info(string.Format("SendWayPoint: {0}, {1}", gpsId, gpsData));

            //vehicleName = ProcessVehicleName(vehicleName);
            var wayPoint = ConvertToGpsData2(gpsId, gpsData);

            if (!m_locks.ContainsKey(gpsId))
                m_locks[gpsId] = new object();

            lock (m_locks[gpsId])
             {
                 using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                 {
                     //var tracks = GetTracksFromVehicleName(gpsId, trackPoint.GpsTime, rep);

                     try
                     {
                         rep.BeginTransaction();

                         Track nowTrack = GetNowTrack(rep, gpsId, wayPoint.GpsTime, false);
                         if (nowTrack != null)
                         {
                             wayPoint.Track = nowTrack;
                             //wayPoint.Action = actionData;
                             SaveGpsData(rep, wayPoint);
                         }

                         rep.CommitTransaction();
                         return true;
                     }
                     catch (Exception)
                     {
                         rep.RollbackTransaction();
                         return false;
                     }
                 }
             }
        }

        private static Dictionary<string, object> m_locks = new Dictionary<string, object>();

        //[WebMethod]
        public bool SendTrackPoint(string gpsId, string gpsData)
        {
            Logger.Info(string.Format("SendTrackPoint: {0}, {1}", gpsId, gpsData));

            //vehicleName = ProcessVehicleName(vehicleName);
            var trackPoint = ConvertToGpsData(gpsId, gpsData);

            if (!m_locks.ContainsKey(gpsId))
                m_locks[gpsId] = new object();

            lock (m_locks[gpsId])
            {
                using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                {
                    try
                    {
                        rep.BeginTransaction();

                        Track nowTrack = GetNowTrack(rep, gpsId, trackPoint.GpsTime, false);
                        Logger.Warn(string.Format("{0}'s Track is null", gpsId));

                        if (nowTrack != null)
                        {
                            trackPoint.Track = nowTrack;
                            SaveGpsData(rep, trackPoint);
                        }
                        rep.CommitTransaction();
                        return true;
                    }
                    catch (Exception ex)
                    {
                        rep.RollbackTransaction();
                        Logger.Error("Error " + ex.Message);
                        return false;
                    }
                }
            }
        }

        ////[WebMethod()]
        //public Track StartRecording(string gpsId, DateTime startTime)
        //{
        //    Logger.Info(string.Format("{0} StartRecording at {1}", gpsId, startTime));

        //    vehicleName = ProcessVehicleName(vehicleName);
        //    using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
        //    {
        //        try
        //        {
        //            rep.BeginTransaction();

        //            var track = GetNowTrack(gpsId, startTime, true, rep);

        //            rep.CommitTransaction();

        //            return track;
        //        }
        //        catch (Exception)
        //        {
        //            rep.RollbackTransaction();

        //            return null;
        //        }
        //    }
        //}

        private static IList<Track> GetTracksFromGpsId(string gpsId, Feng.IRepository rep)
        {
            var tracks = rep.List<Feng.Track>("from Track g where g.VehicleName = :gpsId and EndTime is null and StartTime is not null order by StartTime desc",
                        new Dictionary<string, object> { { "gpsId", gpsId } });
            return tracks;
        }

        private static Track GetNowTrack(Feng.IRepository rep, string gpsId, System.DateTime nowTime, bool newTrack)
        {
            //if (Math.Abs((nowTime - System.DateTime.Now).TotalHours) > 1)
            //    return null;

            IList<Track> tracks = GetTracksFromGpsId(gpsId, rep);
            Track nowTrack = null;
            bool existTrack = false;

            for (int i = 0; i < tracks.Count - 1; ++i)
            {
                var lastTrackPoint = TrackPointDao.GetLastestTrackPoint(tracks[i]);
                DateTime useDate;
                if (lastTrackPoint != null)
                    useDate = lastTrackPoint.GpsTime;
                else
                    useDate = System.DateTime.Now;

                if (i == 0)// && Math.Abs((nowTime - useDate).TotalHours) < 12)
                {
                    nowTrack = tracks[i];
                    existTrack = true;
                    Logger.Warn(string.Format("Use exist track of {0}", tracks[i].ID));
                }
                else
                {
                    tracks[i].EndTime = useDate;
                    s_trackDao.Update(rep, tracks[i]);
                    Logger.Warn(string.Format("track {0} is ended because of new start", tracks[i].ID));
                }
            }

            if (!existTrack && newTrack)
            {
                nowTrack = NewTrack(gpsId);
                nowTrack.StartTime = nowTime;
                s_trackDao.Save(rep, nowTrack);
                Logger.Info(string.Format("Start new track."));
            }
            if (nowTrack == null)
            {
                Logger.Info("nowTrack is null!");
            }
            return nowTrack;
        }

        ////[WebMethod()]
        //public void StopRecording(string gpsId, DateTime endTime)
        //{
        //    Logger.Info(string.Format("{0} StopRecording at {1}", gpsId, endTime));

        //    vehicleName = ProcessVehicleName(vehicleName);

        //    using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
        //    {
        //        try
        //        {
        //            rep.BeginTransaction();

        //            var tracks = GetTracksFromVehicleName(gpsId, rep);
        //            foreach (var track in tracks)
        //            {
        //                if (track.EndTime.HasValue)
        //                    continue;
        //                track.EndTime = endTime;
        //                s_trackDao.Update(rep, tracks[0]);
        //            }

        //            rep.CommitTransaction();
        //        }
        //        catch (Exception)
        //        {
        //            rep.RollbackTransaction();
        //        }
        //    }
        //}

        private static Feng.TrackPointDao s_trackPointDao = new Feng.TrackPointDao();
        private static Feng.TrackDao s_trackDao = new Feng.TrackDao();
        private static Feng.WayPointDao s_wayPointDao = new Feng.WayPointDao();

        private static Track NewTrack(string gpsId)
        {
            //Zkzx.Model.车辆 cl = rep.Get<Zkzx.Model.车辆>(Guid.Parse("888b4dba-c454-42a0-9bbe-9e7500c65101"));
            //Zkzx.Model.车辆作业 clzy = new Zkzx.Model.车辆作业();
            //clzy.车辆 = cl;
            //clzy.专家任务 = rep.Get<Zkzx.Model.专家任务>(Guid.Parse("00000000-0000-0000-0000-000000000001"));
            //clzy.作业号 = s_dao.生成作业号();

            Track track = new Track();
            track.Name = "Default";
            track.VehicleName = gpsId;
            track.IsActive = true;
            return track;
        }

        //private const string s_sendFormat = @"""Action"": ""{1}"", ""Text"": ""{2}"", ""Id"": ""{0}""";

        //[WebMethod]
        //public string SendActionData(string gpsId, string gpsData, string actionData)
        //{
        //    //var ret = rep.Session.CreateQuery("from TrackPoint g where g.VehicleId = :VehicleId and g.MessageTime <= :GPSTime order by g.GPSTime desc")
        //    //    .SetString("VehicleId", gpsId)
        //    //    .SetDateTime("GPSTime", System.DateTime.Now)
        //    //    .SetMaxResults(1)
        //    //    .List<Feng.Gps.TrackPoint>();
        //    //if (ret.Count == 0)
        //    //{
        //    //    return null;
        //    //}

        //    try
        //    {
        //        if (string.IsNullOrEmpty(actionData))
        //            return null;

        //        //Zkzx.Model.车辆作业 clzy = null;
        //        Feng.Gps.Track clzy = null;
        //        string ret = null;
        //        string nowAction = null;

        //        var entity = ConvertToGpsData(gpsId, gpsData);

        //        Newtonsoft.Json.Linq.JObject o = Newtonsoft.Json.Linq.JObject.Parse(actionData);
        //        string nowActionId = (string)o["Action"];
        //        string sId = (string)o["Id"];
        //        //Guid? zyid = string.IsNullOrEmpty(sId) ? null : (Guid?)Guid.Parse(sId);
        //        long? zyid = string.IsNullOrEmpty(sId) ? null : (long?)long.Parse(sId);
        //        TodoAction currentAction = null;

        //        using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
        //        {
        //            if (string.IsNullOrEmpty(nowActionId) || nowActionId == "0" || !zyid.HasValue)
        //            {
        //                //Zkzx.Model.车辆 cl = rep.Get<Zkzx.Model.车辆>(Guid.Parse("888b4dba-c454-42a0-9bbe-9e7500c65101"));

        //                //var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE 车辆.车牌号 = :车牌号 AND 结束时间 IS NULL",
        //                //    new Dictionary<string, object> { { "车牌号", "浙B-8092H" } });
        //                //if (list.Count > 0)
        //                //{
        //                //    clzy = list[0];
        //                //    if (!clzy.开始时间.HasValue)
        //                //    {
        //                //        currentAction = TodoActions.Instance.Actions[0];
        //                //        //ret = @"""Action"": ""1"", ""Text"": ""开始"", ""Id"": ""{0}""";
        //                //    }
        //                //    else if (!clzy.起始地时间.HasValue)
        //                //    {
        //                //        currentAction = TodoActions.Instance.Actions[1];
        //                //        //ret = @"""Action"": ""2"", ""Text"": ""提箱"", ""Id"": ""{0}""";
        //                //    }
        //                //    else if (!clzy.途径地时间.HasValue)
        //                //    {
        //                //        currentAction = TodoActions.Instance.Actions[2];
        //                //        //ret = @"""Action"": ""3"", ""Text"": ""卸货"", ""Id"": ""{0}""";
        //                //    }
        //                //    else if (!clzy.终止地时间.HasValue)
        //                //    {
        //                //        currentAction = TodoActions.Instance.Actions[3];
        //                //        //ret = @"""Action"": ""4"", ""Text"": ""还箱"", ""Id"": ""{0}""";
        //                //    }
        //                //    else if (!clzy.结束时间.HasValue)
        //                //    {
        //                //        currentAction = TodoActions.Instance.Actions[4];
        //                //        //ret = @"""Action"": ""9"", ""Text"": ""结束"", ""Id"": ""{0}""";
        //                //    }
        //                //    else
        //                //    {
        //                //        throw new ArgumentException("Invalid search!");
        //                //    }
        //                //}
        //                //else
        //                //{
        //                //    clzy = NewZy(rep);
        //                //    //ret = @"""Action"": ""1"", ""Text"": ""开始"", ""Id"": ""{0}""";
        //                //    nowAction = null;

        //                //    currentAction = TodoActions.Instance.Actions[0];
        //                //}

        //                var list = rep.List<Feng.Gps.Track>("FROM Track WHERE VehicleName = :gpsId AND EndTime IS NULL",
        //                    new Dictionary<string, object> { { "gpsId", gpsId } });
        //                if (list.Count > 0)
        //                {
        //                    clzy = list[0];
        //                    if (!clzy.StartTime.HasValue)
        //                    {
        //                        currentAction = NormalActions.Instance.Actions[0];
        //                    }
        //                    else if (!clzy.EndTime.HasValue)
        //                    {
        //                        currentAction = NormalActions.Instance.Actions[1];
        //                    }
        //                    else
        //                    {
        //                        throw new ArgumentException("Invalid search!");
        //                    }
        //                }
        //                else
        //                {
        //                    //clzy = NewZy(rep);
        //                    //ret = @"""Action"": ""1"", ""Text"": ""开始"", ""Id"": ""{0}""";

        //                    clzy = NewTrack(gpsId);
        //                    nowAction = null;

        //                    currentAction = NormalActions.Instance.Actions[0];
        //                }
        //            }
        //            //else if (nowActionId == "1")
        //            //{
        //            //    var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE Id = :Id",
        //            //        new Dictionary<string, object> { { "Id", zyid } });
        //            //    if (list.Count > 0)
        //            //    {
        //            //        clzy = list[0];
        //            //        clzy.开始时间 = entity.LocalGpsTime;

        //            //        currentAction = TodoActions.Instance.Actions[0];

        //            //        //ret = @"""Action"": ""2"", ""Text"": ""提箱"", ""Id"": ""{0}""";
        //            //        nowAction = currentAction.Text;
        //            //        currentAction = TodoActions.Instance.Actions[1];
        //            //    }
        //            //    else
        //            //    {
        //            //        clzy = NewZy(rep);
        //            //        nowAction = null;
        //            //    }
        //            //}
        //            //else if (nowActionId == "2")
        //            //{
        //            //    var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE Id = :Id",
        //            //        new Dictionary<string, object> { { "Id", zyid } });
        //            //    if (list.Count > 0)
        //            //    {
        //            //        clzy = list[0];
        //            //        clzy.起始地时间 = entity.LocalGpsTime;

        //            //        currentAction = TodoActions.Instance.Actions[1];
        //            //        //ret = @"""Action"": ""3"", ""Text"": ""卸货"", ""Id"": ""{0}""";
        //            //        //nowAction = "提箱";
        //            //        nowAction = currentAction.Text;
        //            //        currentAction = TodoActions.Instance.Actions[2];
        //            //    }
        //            //    else
        //            //    {
        //            //        clzy = NewZy(rep);
        //            //        nowAction = null;
        //            //    }
        //            //}
        //            //else if (nowActionId == "3")
        //            //{
        //            //    var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE Id = :Id",
        //            //       new Dictionary<string, object> { { "Id", zyid } });
        //            //    if (list.Count > 0)
        //            //    {
        //            //        clzy = list[0];
        //            //        clzy.途径地时间 = entity.LocalGpsTime;

        //            //        currentAction = TodoActions.Instance.Actions[2];
        //            //        //ret = @"""Action"": ""4"", ""Text"": ""还箱"", ""Id"": ""{0}""";
        //            //        //nowAction = "卸货";
        //            //        nowAction = currentAction.Text;
        //            //        currentAction = TodoActions.Instance.Actions[3];
        //            //    }
        //            //    else
        //            //    {
        //            //        clzy = NewZy(rep);
        //            //        nowAction = null;
        //            //    }
        //            //}
        //            //else if (nowActionId == "4")
        //            //{
        //            //    var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE Id = :Id",
        //            //       new Dictionary<string, object> { { "Id", zyid } });
        //            //    if (list.Count > 0)
        //            //    {
        //            //        clzy = list[0];
        //            //        clzy.终止地时间 = entity.LocalGpsTime;

        //            //        currentAction = TodoActions.Instance.Actions[3];
        //            //        //ret = @"""Action"": ""9"", ""Text"": ""结束"", ""Id"": ""{0}""";
        //            //        //nowAction = "还箱";
        //            //        nowAction = currentAction.Text;
        //            //        currentAction = TodoActions.Instance.Actions[4];
        //            //    }
        //            //    else
        //            //    {
        //            //        clzy = NewZy(rep);
        //            //        nowAction = null;
        //            //    }
        //            //}
        //            //else if (nowActionId == "9")
        //            //{
        //            //    var list = rep.List<Zkzx.Model.车辆作业>("FROM 车辆作业 WHERE Id = :Id",
        //            //       new Dictionary<string, object> { { "Id", zyid } });
        //            //    if (list.Count > 0)
        //            //    {
        //            //        clzy = list[0];
        //            //        clzy.结束时间 = entity.LocalGpsTime;

        //            //        currentAction = TodoActions.Instance.Actions[4];
        //            //        //ret = @"""Action"": ""0"", ""Text"": ""获取任务"", ""Id"": ""{0}""";
        //            //        //nowAction = "结束";
        //            //        nowAction = currentAction.Text;
        //            //        currentAction = null;
        //            //    }
        //            //    else
        //            //    {
        //            //        clzy = NewZy(rep);
        //            //        nowAction = null;
        //            //    }
        //            //}
        //            else if (nowActionId == "1")
        //            {
        //                var list = rep.List<Feng.Gps.Track>("FROM Track WHERE Id = :Id",
        //                    new Dictionary<string, object> { { "Id", zyid } });
        //                if (list.Count > 0)
        //                {
        //                    clzy = list[0];
        //                    clzy.StartTime = entity.LocalGpsTime;

        //                    currentAction = NormalActions.Instance.Actions[0];
        //                    nowAction = currentAction.Text;
        //                    currentAction = NormalActions.Instance.Actions[1];
        //                }
        //                else
        //                {
        //                    clzy = NewTrack(gpsId);
        //                    nowAction = null;
        //                }
        //            }
        //            else if (nowActionId == "9")
        //            {
        //                var list = rep.List<Feng.Gps.Track>("FROM Track WHERE Id = :Id",
        //                    new Dictionary<string, object> { { "Id", zyid } });
        //                if (list.Count > 0)
        //                {
        //                    clzy = list[0];
        //                    clzy.EndTime = entity.LocalGpsTime;

        //                    currentAction = NormalActions.Instance.Actions[1];

        //                    nowAction = currentAction.Text;
        //                    currentAction = null;
        //                }
        //                else
        //                {
        //                    clzy = NewTrack(gpsId);
        //                    nowAction = null;
        //                }
        //            }

        //            if (clzy != null)
        //            {
        //                try
        //                {
        //                    rep.BeginTransaction();
        //                    s_trackDao.SaveOrUpdate(rep, clzy);

        //                    if (!string.IsNullOrEmpty(nowAction))
        //                    {
        //                        entity.Action = nowAction;
        //                        SaveGpsData(rep, entity);
        //                    }

        //                    rep.CommitTransaction();
        //                }
        //                catch (Exception)
        //                {
        //                    rep.RollbackTransaction();
        //                    currentAction = null;
        //                    ret = null;
        //                }
        //            }

        //            if (currentAction != null)
        //            {
        //                //ret = string.Format(ret, clzy.Identity);
        //                ret = string.Format(s_sendFormat, clzy.Identity, currentAction.Action, currentAction.Text);
        //            }
        //            else
        //            {
        //                ret = string.Format(s_sendFormat, clzy.Identity, "0", "获取任务");
        //            }
        //            ret = "{" + ret + "}";
        //        }
        //        return ret;
        //    }
        //    catch (Exception ex)
        //    {
        //        Console.WriteLine(ex.Message);
        //        Console.WriteLine(ex.StackTrace);
        //    }
        //    return null;
        //}
    }
}
