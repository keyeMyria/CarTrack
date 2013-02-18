using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace CarTrackService
{
    public class TodoAction
    {
        public int Action
        {
            get;
            set;
        }

        public string Text
        {
            get;
            set;
        }

        public TodoAction Next
        {
            get;
            set;
        }
    }
}