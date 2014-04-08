/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.app.video;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.apache.commons.lang3.StringUtils;

public class VideoApplication extends MultiThreadedApplicationAdapter {
	private static Logger log = Red5LoggerFactory.getLogger(VideoApplication.class, "video");
	
	private IScope appScope;
	private IServerStream serverStream;
	
	private boolean recordVideoStream = false;
	private EventRecordingService recordingService;
	private final Map<String, IStreamListener> streamListeners = new HashMap<String, IStreamListener>();

    private Map<String, CustomStreamRelay> remoteStreams = new ConcurrentHashMap<String, CustomStreamRelay>();
    private Map<String, Integer> listenersOnRemoteStream = new ConcurrentHashMap<String, Integer>();

	
    @Override
	public boolean appStart(IScope app) {
	    super.appStart(app);
		log.info("oflaDemo appStart");
		System.out.println("oflaDemo appStart");    	
		appScope = app;
		return true;
	}

    @Override
	public boolean appConnect(IConnection conn, Object[] params) {
		log.info("oflaDemo appConnect"); 
       
        return super.appConnect(conn, params);
	}

    @Override
	public void appDisconnect(IConnection conn) {
		log.info("oflaDemo appDisconnect");
		if (appScope == conn.getScope() && serverStream != null) {
			serverStream.close();
		}
		super.appDisconnect(conn);
	}
    
    @Override
    public void streamPublishStart(IBroadcastStream stream) {
    	super.streamPublishStart(stream);
    }
    

    public IBroadcastScope getBroadcastScope(IScope scope, String name) {
    IBasicScope basicScope = scope.getBasicScope(ScopeType.BROADCAST, name);
    if (!(basicScope instanceof IBroadcastScope)) {
        return null;
    } else {
        return (IBroadcastScope) basicScope;
    }
}


    @Override
    public void streamBroadcastStart(IBroadcastStream stream) {
    	IConnection conn = Red5.getConnectionLocal();  
    	super.streamBroadcastStart(stream);
    	log.info("streamBroadcastStart " + stream.getPublishedName() + " " + System.currentTimeMillis() + " " + conn.getScope().getName());
        


        /*String sourceServer = "143.54.10.63";
        String sourceStreamName = stream.getPublishedName();
        String destinationServer = "143.54.10.63";
        String destinationStreamName = "320x240-teste";
        String app = "video/"+Red5.getConnectionLocal().getScope().getName();
        
        CustomStreamRelay remoteRelay = new CustomStreamRelay();
        remoteRelay.initRelay(new String[]{sourceServer, app, sourceStreamName, destinationServer, "video", destinationStreamName, "live"});
        remoteRelay.startRelay();
                
        */
        
        if (recordVideoStream &&  stream.getPublishedName().contains("/") == false) {
	    	recordStream(stream);
	    	VideoStreamListener listener = new VideoStreamListener(); 
	        listener.setEventRecordingService(recordingService);
	        stream.addStreamListener(listener); 
	        streamListeners.put(conn.getScope().getName() + "-" + stream.getPublishedName(), listener);
        }
    }

    @Override
    public void streamBroadcastClose(IBroadcastStream stream) {
    	IConnection conn = Red5.getConnectionLocal();  
    	super.streamBroadcastClose(stream);
    	
    	if (recordVideoStream) {
    		IStreamListener listener = streamListeners.remove(conn.getScope().getName() + "-" + stream.getPublishedName());
    		if (listener != null) {
    			stream.removeStreamListener(listener);
    		}
    		
        	long publishDuration = (System.currentTimeMillis() - stream.getCreationTime()) / 1000;
        	log.info("streamBroadcastClose " + stream.getPublishedName() + " " + System.currentTimeMillis() + " " + conn.getScope().getName());
    		Map<String, String> event = new HashMap<String, String>();
    		event.put("module", "WEBCAM");
    		event.put("timestamp", new Long(System.currentTimeMillis()).toString());
    		event.put("meetingId", conn.getScope().getName());
    		event.put("stream", stream.getPublishedName());
    		event.put("duration", new Long(publishDuration).toString());
    		event.put("eventName", "StopWebcamShareEvent");
    		
    		recordingService.record(conn.getScope().getName(), event);    		
    	}
    }
    
    /**
     * A hook to record a stream. A file is written in webapps/video/streams/
     * @param stream
     */
    private void recordStream(IBroadcastStream stream) {
    	IConnection conn = Red5.getConnectionLocal();   
    	long now = System.currentTimeMillis();
    	String recordingStreamName = stream.getPublishedName(); // + "-" + now; /** Comment out for now...forgot why I added this - ralam */
     
    	try {    		
    		log.info("Recording stream " + recordingStreamName );
    		ClientBroadcastStream cstream = (ClientBroadcastStream) this.getBroadcastStream(conn.getScope(), stream.getPublishedName());
    		cstream.saveAs(recordingStreamName, false);
    	} catch(Exception e) {
    		log.error("ERROR while recording stream " + e.getMessage());
    		e.printStackTrace();
    	}    	
    }

	public void setRecordVideoStream(boolean recordVideoStream) {
		this.recordVideoStream = recordVideoStream;
	}
	
	public void setEventRecordingService(EventRecordingService s) {
		recordingService = s;
	}

    @Override
    public synchronized void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item, boolean isLive) {
        // log w3c connect event
        String streamName = item.getName();
        
        //rtmp://SV1/video/conferencia
        //SV2/SV3/SV4/streamName


        if(streamName.contains("/"))
            if(remoteStreams.containsKey(streamName) == false) {
                String[] parts = streamName.split("/");
                String sourceServer = parts[0];
                String sourceStreamName = StringUtils.join(parts, '/', 1, parts.length);
                String destinationServer = Red5.getConnectionLocal().getHost();
                String destinationStreamName = streamName;
                String app = "video/"+Red5.getConnectionLocal().getScope().getName();
                
                CustomStreamRelay remoteRelay = new CustomStreamRelay();
                remoteRelay.initRelay(new String[]{sourceServer, app, sourceStreamName, destinationServer, app, destinationStreamName, "live"});
                remoteRelay.startRelay();
                remoteStreams.put(destinationStreamName, remoteRelay);
                listenersOnRemoteStream.put(streamName, 1);
            }
            else {
                Integer numberOfListeners = listenersOnRemoteStream.get(streamName) + 1;
                listenersOnRemoteStream.put(streamName,numberOfListeners);
            }
        
        log.info("W3C x-category:stream x-event:play c-ip:{} x-sname:{} x-name:{}", new Object[] { Red5.getConnectionLocal().getRemoteAddress(), stream.getName(), item.getName() });
    }

    @Override
    public synchronized void streamSubscriberClose(ISubscriberStream stream) {
        super.streamSubscriberClose(stream);
        String streamName = stream.getBroadcastStreamPublishName();
        if(streamName.contains("/"))
            if(remoteStreams.containsKey(streamName)) {
                Integer numberOfListeners = listenersOnRemoteStream.get(streamName);
                if(numberOfListeners != null) {
                    if(numberOfListeners > 1) {
                        numberOfListeners = numberOfListeners - 1;
                        listenersOnRemoteStream.put(streamName, numberOfListeners);
                    }
                    else {
                        listenersOnRemoteStream.remove(streamName);
                        CustomStreamRelay remoteRelay = remoteStreams.remove(streamName);
                        remoteRelay.stopRelay();
                    }
                }

            }
    }

}
