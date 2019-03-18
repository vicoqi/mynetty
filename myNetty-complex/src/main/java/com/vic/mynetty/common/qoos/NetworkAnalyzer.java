package com.vic.mynetty.common.qoos;


import com.vic.mynetty.netty_client.nettyclient.AbstractClientSession;
import com.vic.mynetty.netty_client.config.ClientSpecialConfig;

//todo 这个类感觉
public class NetworkAnalyzer {
	private AbstractClientSession session;
	private int status = 0;
	private long shortestDelay = -1;
	private int netDelayMultiplier = -1;
	private int sameEventCount;
	private long networkDelayMills = -1;
	private int netDelayEventCount;
	public NetworkAnalyzer(AbstractClientSession session, ClientSpecialConfig commConfig) {
		this.session = session;
		this.netDelayMultiplier = commConfig.getNetDelayMultiplier();
		this.netDelayEventCount = commConfig.getNetDelayEventCount();
		this.networkDelayMills = commConfig.getNetworkDelayMills();
	}

	public void analysis(long delay) {
		if (shortestDelay == -1) {
			shortestDelay = delay;
			return;
		}
		if ((networkDelayMills > -1 && delay > networkDelayMills) 
				|| (netDelayMultiplier > -1 && delay > shortestDelay * netDelayMultiplier)) {
			switch (status) {
			case 0:
			case 2:
				status = 1;
				sameEventCount = 1;
				return;
			case 1:
				sameEventCount ++;
				if (sameEventCount > netDelayEventCount) {
					session.getEventfirer().fireSessionNetDelay();
					status = 0;
					sameEventCount = 0;
				}
				return;
			default:
				break;
			}
		} else {
			if (delay < shortestDelay) {
				delay = shortestDelay;
			}
			switch (status) {
			case 0:
			case 1:
				status = 2;
				sameEventCount = 1;
				return;
			case 2:
				sameEventCount ++;
				if (sameEventCount > netDelayEventCount) {
					session.getEventfirer().fireSessionNetRecover();
					status = 0;
					sameEventCount = 0;
				}
				return;
			default:
				break;
			}
		}
	}
	
	public static void main(String[] args) {
		ClientSpecialConfig commConfig = new ClientSpecialConfig();
		NetworkAnalyzer na = new NetworkAnalyzer(null, commConfig);
		na.analysis(10);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
		na.analysis(5);
		na.analysis(16);
		na.analysis(20);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
		na.analysis(101);
	}
}
