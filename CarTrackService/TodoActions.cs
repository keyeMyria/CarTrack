using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;

namespace CarTrackService
{
    public class NormalActions : Feng.Singleton<NormalActions>
    {
        private IList<TodoAction> m_actions;

        public IList<TodoAction> Actions
        {
            get { return m_actions; }
        }

        public NormalActions()
        {
            m_actions = new List<TodoAction>{ 
                    new TodoAction
                    {
                        Action = 1,
                        Text = "开始"
                    },
                    new TodoAction
                    {
                        Action = 9,
                        Text = "结束"
                    },
                };

            for (int i = 0; i < m_actions.Count - 1; ++i)
            {
                m_actions[i].Next = m_actions[i + 1];
            }
        }
    }

    public class 进口Actions : Feng.Singleton<进口Actions>
    {
        private IList<TodoAction> m_actions;

        public IList<TodoAction> Actions
        {
            get { return m_actions; }
        }

        public 进口Actions()
        {
            m_actions = new List<TodoAction>{ 
                    new TodoAction
                    {
                        Action = 1,
                        Text = "开始"
                    },
                    new TodoAction
                    {
                        Action = 2,
                        Text = "提箱"
                    },
                    new TodoAction
                    {
                        Action = 3,
                        Text = "卸货"
                    },
                    new TodoAction
                    {
                        Action = 4,
                        Text = "还箱"
                    },
                    new TodoAction
                    {
                        Action = 9,
                        Text = "结束"
                    },
                };

            for (int i = 0; i < m_actions.Count - 1; ++i)
            {
                m_actions[i].Next = m_actions[i + 1];
            }
        }
    }
}