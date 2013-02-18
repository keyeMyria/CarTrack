using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.ServiceModel;
using System.Text;
using System.IO;
using Newtonsoft.Json;
using Feng;
using Zkzx.Model;

namespace CarTrackService
{
    public class ZkzxDataService : GpsDataService, IZkzxDataService
    {
        private void DisableCache()
        {
            //System.ServiceModel.Web.WebOperationContext.Current.OutgoingResponse.Headers.Add("Cache-Control", "no-cache");
            //System.ServiceModel.Web.WebOperationContext.Current.OutgoingResponse.Headers.Add("Pragma", "no-cache");

            System.ServiceModel.Web.WebOperationContext.Current.OutgoingResponse.Headers.Remove("Server");
        }
        public StringValue GetAppVersionCode()
        {
            DisableCache();
            Logger.Info("GetAppVersionCode");
            return new StringValue { Value = "2" };
        }

        public StringValue GetWorkSequence(string sWorkerId)
        {
            DisableCache();
            Logger.Info(string.Format("GetWorkSequence: {0}", sWorkerId));

            StringBuilder sb = new StringBuilder();
            try
            {
                using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                {
                    var clzy = rep.Get<Zkzx.Model.车辆作业>(Guid.Parse(sWorkerId));
                    if (clzy != null)
                    {
                        int[] taskIdx = null;
                        string[] importantAreas = null;
                        string[] importantTaskStatus = null;
                        string[] importantWorkStatus = null;
                        string workTitle = ModelHelper.Get任务状态(clzy.专家任务, out taskIdx, out importantAreas, out importantTaskStatus, out importantWorkStatus);

                        //clzy.最新作业状态.任务进程
                        StringWriter sw = new StringWriter(sb);
                        using (JsonWriter writer = new JsonTextWriter(sw))
                        {
                            writer.Formatting = Formatting.Indented;

                            writer.WriteStartObject();

                            writer.WritePropertyName("Title");
                            writer.WriteValue(workTitle);
                            writer.WritePropertyName("Actions");
                            writer.WriteStartArray();
                            foreach (string s in importantWorkStatus)
                            {
                                writer.WriteValue(s);
                            }
                            writer.WriteEnd();
                            writer.WritePropertyName("TaskIdxs");
                            writer.WriteStartArray();
                            foreach (int s in taskIdx)
                            {
                                writer.WriteValue(s);
                            }
                            writer.WriteEnd();
                            writer.WritePropertyName("ActionIdx");
                            if (!clzy.开始时间.HasValue || clzy.结束时间.HasValue)
                                writer.WriteValue(-1);
                            else
                                writer.WriteValue(clzy.最新作业状态.作业进程序号);
                            writer.WritePropertyName("ActionIdxIdx");
                            if (!string.IsNullOrEmpty(clzy.最新作业状态.作业状态))
                            {
                                if (clzy.最新作业状态.作业状态.EndsWith("途中"))
                                    writer.WriteValue(1);
                                else if (clzy.最新作业状态.作业状态.EndsWith("中"))
                                    writer.WriteValue(0);
                                else
                                    writer.WriteValue(-1);
                            }
                            else
                            {
                                writer.WriteValue(1);
                            }

                            writer.WritePropertyName("Xianghao");
                            writer.WriteStartArray();
                            foreach (var rw in clzy.专家任务.任务)
                            {
                                writer.WriteValue(string.IsNullOrEmpty(rw.箱号) ? string.Empty : rw.箱号);
                            }
                            writer.WriteEnd();

                            writer.WritePropertyName("Fengzihao");
                            writer.WriteStartArray();
                            foreach (var rw in clzy.专家任务.任务)
                            {
                                writer.WriteValue(string.IsNullOrEmpty(rw.封志号) ? string.Empty : rw.封志号);
                            }
                            writer.WriteEnd();

                            writer.WriteEndObject();
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                ExceptionProcess.ProcessWithResume(ex);
            }
            return new StringValue { Value = sb.ToString() };
        }

        public StringValue GetWorkDetails(string sWorkerId, string sActionIdx)
        {
            DisableCache();
            Logger.Info(string.Format("GetWorkDetails: {0}, {1}", sWorkerId, sActionIdx));

            StringBuilder sb = new StringBuilder();
            int actionIdx = Convert.ToInt32(sActionIdx);
            if (actionIdx < 0)
                return null;

            try
            {
                using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                {
                    var clzy = rep.Get<Zkzx.Model.车辆作业>(Guid.Parse(sWorkerId));
                    if (clzy != null)
                    {
                        int[] taskIdx = null;
                        string[] importantAreas = null;
                        string[] importantTaskStatus = null;
                        string[] importantWorkStatus = null;
                        ModelHelper.Get任务状态(clzy.专家任务, out taskIdx, out importantAreas, out importantTaskStatus, out importantWorkStatus);
                        if (actionIdx >= taskIdx.Length)
                            return null;

                        string address = null;
                        string tel = null;
                        string detail = null;
                        var currentRw = clzy.专家任务.任务[taskIdx[actionIdx]];
                        if (importantWorkStatus[actionIdx].Contains("提箱"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部", currentRw.提箱点编号);
                            detail = currentRw.提箱时间要求.HasValue ? "提箱时间要求: " + currentRw.提箱时间要求.ToString() : "";
                        }
                        else if (importantWorkStatus[actionIdx].Contains("还箱") || importantWorkStatus[actionIdx].Contains("进港"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部", currentRw.还箱进港点编号);
                            detail = currentRw.还箱进港时间要求.HasValue ? "还箱进港时间要求: " + currentRw.还箱进港时间要求.ToString() : "";
                        }
                        else if (importantWorkStatus[actionIdx].Contains("卸货"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部", currentRw.卸货地编号);
                            address += ", " + currentRw.卸货地详细地址;
                            tel = currentRw.卸货联系人 + "," + currentRw.卸货联系手机 + ", " + currentRw.卸货联系座机;
                            detail = currentRw.卸货时间要求始.HasValue ? "卸货时间要求始: " + currentRw.卸货时间要求始.ToString() : "";
                            detail += System.Environment.NewLine;
                            detail += currentRw.卸货时间要求止.HasValue ? "卸货时间要求止: " + currentRw.卸货时间要求止.ToString() : "";
                        }
                        else if (importantWorkStatus[actionIdx].Contains("装货"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部", currentRw.装货地编号);
                            address += ", " + currentRw.装货地详细地址;
                            tel = currentRw.装货联系人 + "," + currentRw.装货联系手机 + ", " + currentRw.装货联系座机;
                            detail = currentRw.装货时间要求始.HasValue ? "装货时间要求始: " + currentRw.装货时间要求始.ToString() : "";
                            detail += System.Environment.NewLine;
                            detail += currentRw.装货时间要求止.HasValue ? "装货时间要求止: " + currentRw.装货时间要求止.ToString() : "";
                        }
                        else if (importantWorkStatus[actionIdx].Contains("施关封"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部",
                                Feng.Utils.NameValueControlHelper.GetMultiString("港区指运地_施封地", currentRw.提箱点编号));

                        }
                        else if (importantWorkStatus[actionIdx].Contains("验关封"))
                        {
                            address = Feng.NameValueMappingCollection.Instance.FindNameFromId("人员单位_装卸货地_全部",
                                Feng.Utils.NameValueControlHelper.GetMultiString("港区指运地_施封地", currentRw.卸货地编号));
                        }

                        //clzy.最新作业状态.任务进程
                        StringWriter sw = new StringWriter(sb);
                        using (JsonWriter writer = new JsonTextWriter(sw))
                        {
                            writer.Formatting = Formatting.Indented;

                            writer.WriteStartObject();
                            writer.WritePropertyName("Address");
                            writer.WriteValue(address);
                            writer.WritePropertyName("Tel");
                            writer.WriteValue(tel);
                            writer.WritePropertyName("Detail");
                            writer.WriteValue(detail);
                            writer.WriteEndObject();
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                ExceptionProcess.ProcessWithResume(ex);
            }
            return new StringValue { Value = sb.ToString() };
        }

        public StringValue GetCurrentWorkerIdByTruckId(string sTruckId)
        {
            return GetWorkerIdByTruckId(sTruckId, null);
        }

        public StringValue GetWorkerIdByTruckId(string sTruckId, string sIdx)
        {
            DisableCache();
            Logger.Info(string.Format("GetWorkerIdByTruckId: {0}, {1}", sTruckId, sIdx));

            int idx = 0;
            if (!string.IsNullOrEmpty(sIdx))
                idx = Convert.ToInt32(sIdx);

            using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
            {
                var tracks = rep.List<Zkzx.Model.车辆作业>("from 车辆作业 g where g.车载Id号 = :gpsId and 结束时间 is null order by case when 开始时间 is null then 1 else 0 end, 开始时间, 作业号 desc",
                            new Dictionary<string, object> { { "gpsId", sTruckId } });
                if (tracks.Count > idx && idx >= 0)
                    return new StringValue { Value = tracks[idx].ID.ToString() };
                else
                    return new StringValue { Value = string.Empty };
            }
        }

        public StringValue SendTruckState(string sWorkerId, string state)
        {
            DisableCache();
            Logger.Info(string.Format("SendTruckState: {0}, {1}", sWorkerId, state));

            StringValue okValue = new StringValue { Value = "Ok" };

            DateTime nowDate = System.DateTime.Now;
            using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
            {
                try
                {
                    rep.BeginTransaction();

                    var clzy = rep.Get<车辆作业>(new Guid(sWorkerId));
                    if (clzy != null && clzy.开始时间.HasValue)
                    {
                        if (state == "途中休息" || state == "堵车" || state == "故障处理")
                        {
                            作业异常Dao 作业异常Dao = new 作业异常Dao();
                            作业异常Dao.新作业异常(clzy, state, null, nowDate);
                        }
                        else if (state.StartsWith("动作"))
                        {
                            string[] sIdxs = state.Split(new char[] { '-' });
                            int idx = Convert.ToInt32(sIdxs[1]);
                            int idx2 = Convert.ToInt32(sIdxs[2]);

                            int[] taskIdx = null;
                            string[] importantAreas = null;
                            string[] importantTaskStatus = null;
                            string[] importantWorkStatus = null;

                            ModelHelper.Get任务状态(clzy.专家任务, out taskIdx, out importantAreas, out importantTaskStatus, out importantWorkStatus);
                            if (idx >= 0 && idx < importantAreas.Length)
                            {
                                作业监控Dao zyjkDao = new 作业监控Dao();
                                zyjkDao.更新作业监控状态2(rep, clzy, nowDate, importantAreas[idx], idx2 == 0 ? "开始" : "结束");

                                动作时间数据 entity2 = new 动作时间数据();
                                entity2.时间 = nowDate;
                                entity2.地点 = importantAreas[idx];
                                entity2.动作 = (idx2 == 0 ? "开始" : "结束") + importantWorkStatus[idx];
                                entity2.车辆作业 = clzy;
                                Zkzx.Model.BaseDao<动作时间数据> dao = new Zkzx.Model.BaseDao<动作时间数据>();
                                dao.Save(rep, entity2);
                            }
                            else
                            {
                                okValue = null;
                            }
                        }
                        else if (state.StartsWith("箱号"))
                        {
                            string[] sIdxs = state.Split(new char[] { '-' });
                            int idx = Convert.ToInt32(sIdxs[1]);

                            int[] taskIdx = null;
                            string[] importantAreas = null;
                            string[] importantTaskStatus = null;
                            string[] importantWorkStatus = null;

                            ModelHelper.Get任务状态(clzy.专家任务, out taskIdx, out importantAreas, out importantTaskStatus, out importantWorkStatus);
                            if (idx >= 0 && idx < importantAreas.Length && !string.IsNullOrEmpty(sIdxs[2]))
                            {
                                var rw = clzy.专家任务.任务[taskIdx[idx]];
                                rw.箱号 = sIdxs[2];
                                (new Zkzx.Model.任务Dao()).Update(rep, rw);
                            }
                            else
                            {
                                okValue = null;
                            }
                        }
                        else if (state.StartsWith("封号"))
                        {
                            string[] sIdxs = state.Split(new char[] { '-' });
                            int idx = Convert.ToInt32(sIdxs[1]);

                            int[] taskIdx = null;
                            string[] importantAreas = null;
                            string[] importantTaskStatus = null;
                            string[] importantWorkStatus = null;

                            ModelHelper.Get任务状态(clzy.专家任务, out taskIdx, out importantAreas, out importantTaskStatus, out importantWorkStatus);
                            if (idx >= 0 && idx < importantAreas.Length && !string.IsNullOrEmpty(sIdxs[2]))
                            {
                                var rw = clzy.专家任务.任务[taskIdx[idx]];
                                rw.封志号 = sIdxs[2];
                                (new Zkzx.Model.任务Dao()).Update(rep, rw);
                            }
                            else
                            {
                                okValue = null;
                            }
                        }
                    }
                    else
                    {
                        okValue = null;
                    }
                    rep.CommitTransaction();

                    return okValue;
                }
                catch (Exception)
                {
                    rep.RollbackTransaction();
                }
            }
            return null;
        }

        private static Dictionary<string, object> m_locks = new Dictionary<string, object>();
        private static Feng.TrackPointDao s_trackPointDao = new Feng.TrackPointDao();
        private static 作业监控Dao s_zyjkDao = new 作业监控Dao();
        private void OnTrackPoint(string gpsId, string gpsData)
        {
            Logger.Info(string.Format("{0} send trackPoint of {1}", gpsId, gpsData));

            //vehicleName = ProcessVehicleName(vehicleName);
            var trackPoint = GpsService.ConvertToGpsData(gpsId, gpsData);

            if (!m_locks.ContainsKey(gpsId))
                m_locks[gpsId] = new object();

            lock (m_locks[gpsId])
            {
                车辆作业 selectedClzy = null;
                using (Feng.IRepository rep = new Feng.NH.Repository("zkzx.model.config"))
                {
                    try
                    {
                        rep.BeginTransaction();

                        var clzys = rep.List<车辆作业>("from 车辆作业 g where g.车载Id号 = :gpsId and Track is not null and 结束时间 is null and 开始时间 is not null order by 开始时间 desc",
                            new Dictionary<string, object> { { "gpsId", gpsId } });

                        if (clzys.Count == 0)
                        {
                            //Logger.Warn(string.Format("{0}'s Track is null", gpsId));
                            //return;
                        }
                        else
                        {
                            selectedClzy = clzys[0];
                            var nowTrack = selectedClzy.Track;
                            trackPoint.Track = rep.Get<Track>(nowTrack.Value);
                        }
                        s_trackPointDao.Save(rep, trackPoint);
                        rep.CommitTransaction();
                    }
                    catch (Exception ex)
                    {
                        rep.RollbackTransaction();
                        Logger.Error("Error " + ex.Message);
                    }
                }

                if (selectedClzy != null)
                {
                    s_zyjkDao.更新作业监控状态1(selectedClzy, trackPoint);
                }
            }
        }

        public override string SendTrackPoint(string gpsId, string gpsData)
        {
            DisableCache();

            try
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
                    OnTrackPoint(gpsId, gpsData);
                    return null;
                }
                else if (ss.Length == 8)
                {
                    Logger.Info(string.Format("SendWayPoint: {0}, {1}", gpsId, gpsData));
                    if (string.IsNullOrEmpty(gpsData))
                    {
                        Logger.Warn("gpsData is null");
                        return string.Empty;
                    }
                    base.SendTrackPoint(gpsId, gpsData);
                    return string.Empty;
                }
                else
                {
                    return null;
                }
            }
            catch (Exception ex)
            {
                ExceptionProcess.ProcessWithResume(ex);
                return null;
            }
        }
    }
}
