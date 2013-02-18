using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;

namespace CarTrackService
{
    /* StreetView: http://www.1stwebdesigner.com/tutorials/google-maps-street-view/

http://gmaps-samples.googlecode.com/svn/trunk/streetview/streetview_directions.html
http://slodge.com/runsatStreetView/viewservers.html?activity=95269*/
    public partial class TrackAnimation : System.Web.UI.Page
    {
        protected void Page_Load(object sender, EventArgs e)
        {
            try
            {
                txtTrackId.Value = Convert.ToInt64(Request.QueryString["TrackId"]).ToString();
            }
            catch (Exception)
            {
                txtTrackId.Value = null;
            }
        }
    }
}