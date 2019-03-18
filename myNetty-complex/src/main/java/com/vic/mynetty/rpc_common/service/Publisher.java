package com.vic.mynetty.rpc_common.service;

import com.vic.mynetty.common.AbstractConnection;
import com.vic.mynetty.common.Connection;
import com.vic.mynetty.common.future.FutureListener;
import com.vic.mynetty.common.future.FutureMapListener;
import com.vic.mynetty.common.message.Message;
import com.vic.mynetty.common.state.SessionState;
import com.vic.mynetty.common.strategyenum.FutureEvent;
import com.vic.mynetty.netty_server.session.NettyServerSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

@AllArgsConstructor
@Slf4j
public class Publisher {
	private Message message;
	private Collection<NettyServerSession> sessions;
	//放置监听器的地方
	private FutureMapListener<Message> futureResult;

	private static  Integer timegap= 1000;


	public void multicast() {
		if (!CollectionUtils.isEmpty(sessions)) {
			for (NettyServerSession session : sessions) {
				if (session.getState() == SessionState.READY) {
					Connection conn = session.route(message.getMapping().getPath());
					log.info("CONNECTION_FOUND|sessionUser=[{}]conn=[{}]", session.getUserId(), conn.getId());
					conn.send(message);
				} else {
					log.info("SESSION_NOT_READY_IGNORE|sessionUser=[{}]", session.getUserId());
				}
			}
		}
	}
	
	public void unicast(String userId) {
		if (!CollectionUtils.isEmpty(sessions)) {
			for (NettyServerSession session : sessions) {
				if (session.getUserId().equals(userId)) {
					if (session.getState() == SessionState.READY) {
						Connection conn = session.route(message.getMapping().getPath());
						log.info("CONNECTION_FOUND|sessionUser=[{}]conn=[{}]", session.getUserId(), conn.getId());

						futureResult.addListener(message.getId(),new FutureListener<Message>() {
							@Override
							public void onEvent(FutureEvent event, Message  t, Exception e) {
//								System.out.println(t);
								switch (event) {
									//服务端接收到小车确认收到消息，onceivece 产生success 事件
									case SUCCESS:
										//todo 进行业务逻辑确认
										log.info("Listener|message push success|messageId=[{}]", t.getCollaborationId());
										break;
									case FAILURE:
										//todo 客户端返回失败消息
										log.info("Listener|client return failure|messageId=[{}]|errorMessage:[{}]", t.getCollaborationId(),e.getMessage());
										break;
									case TIMEOUT:
										break;
									default:
										break;
								}
//								futureResult.getListeners().remove(t.getId());
							}
						});
						((AbstractConnection)conn).send(message,futureResult);
					} else {
						log.info("SESSION_NOT_READY_IGNORE|sessionUser=[{}]", session.getUserId());
					}
				}
			}
		}
	}
	
	public void multicast(Integer gap) throws InterruptedException {
		if (!CollectionUtils.isEmpty(sessions)) {
			for (NettyServerSession session : sessions) {
				if (session.getState() == SessionState.READY) {
					Connection conn = session.route(message.getMapping().getPath());
					log.info("CONNECTION_FOUND|sessionUser=[{}]conn=[{}]", session.getUserId(), conn.getId());
					conn.send(message);
				} else {
					log.info("SESSION_NOT_READY_IGNORE|sessionUser=[{}]", session.getUserId());
				}
			}
		}
	}
}
