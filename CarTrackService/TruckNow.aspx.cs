using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;

namespace CarTrackService
{
    public partial class TruckNow : System.Web.UI.Page
    {
        private string m_trackId;
        public string GetTrackId()
        {
            return m_trackId;
        }
        protected void Page_Load(object sender, EventArgs e)
        {
            try
            {
                m_trackId = Convert.ToInt64(Request.QueryString["TrackId"]).ToString();
            }
            catch (Exception)
            {
                m_trackId = null;
            }
        }
    }
}