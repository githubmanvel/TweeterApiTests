package test



import spock.lang.Specification

import java.rmi.activation.ActivationSystem
import java.text.SimpleDateFormat
import java.time.chrono.ChronoLocalDateTime
import java.util.Locale

import org.codehaus.groovy.control.customizers.ImportCustomizer.Import
import org.json.JSONArray
import org.json.JSONObject
import groovy.util.logging.Slf4j


class TimeLineSpec extends Specification {

	def "Check text fields of Home timeline API response for last 3 tweets"(){

		//count of statuses for check
		def countOfStatuses =3;

		setup:
		ArrayList<String> statusIds = new ArrayList<String>();
		for (int i =0; i < countOfStatuses; i++) {
			statusIds.add(new JSONObject(TweeterAPI.sendStatusUpdateRequest(text +" " + i).getBody()).getString("id_str"));
		}

		when:
		def response = TweeterAPI.sendHomeTimeLineRequest();
		then:
		response.getCode()==200;

		JSONArray arr = new JSONArray(response.getBody());
		for (int i = 0; i < countOfStatuses; i++){

			when:
			JSONObject lastStatus = arr.getJSONObject(i);
			def responseText =  lastStatus.getString("text");

			then:
			responseText == text;
		}

		cleanup:
		for (String id : statusIds) {
			TweeterAPI.sendStatusDestroyRequest(id);
		}


		where:
		text << ["Ordinary text", "Кириллица"]
		// Also can be checked:	"i`~!@#^&*(><?/*"

	}

	def "Check created time field of Home timeline API response for the last tweet"(){

		setup:
		def text ="Check time";
		def responseBody = new JSONObject(TweeterAPI.sendStatusUpdateRequest(text).getBody());
		def statusId = responseBody.getString("id_str");
		def createdTimeUpdateResponse = responseBody.getString("created_at");
		def currentTime=System.currentTimeMillis();

		when:
		def response = TweeterAPI.sendHomeTimeLineRequest();
		JSONArray arr = new JSONArray(response.getBody());
		JSONObject lastStatus = arr.getJSONObject(0);
		def createdTimeLineResponse = lastStatus.getString("created_at");

		SimpleDateFormat dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
		Date date = dateFormat.parse(createdTimeLineResponse);

		then:
		response.getCode()==200;
		createdTimeUpdateResponse==createdTimeLineResponse; //check that create time in update API response is the same as in time line API
		currentTime - date.getTime() < 60000; //check that less ten 1 minute(60s, 60 000ms) passed after sendingRequst and creation time

		cleanup:
		TweeterAPI.sendStatusDestroyRequest(lastStatus.getString("id_str"));

	}


	def "Check retweet count field of Home timeline API response for last tweet"(){

		setup:
		def text ="Check retweet count";
		def statusId = new JSONObject(TweeterAPI.sendStatusUpdateRequest(text).getBody()).getString("id_str");

		when:
		def response = TweeterAPI.sendHomeTimeLineRequest();
		JSONArray arr = new JSONArray(response.getBody());
		def retweetCount  = arr.getJSONObject(0).getString("retweet_count");

		then:
		response.getCode()==200;
		retweetCount == "0";

		when:
		TweeterAPI.sendStatusRetweetRequest(statusId); //do retweet
		response= TweeterAPI.sendHomeTimeLineRequest(); //get again time line
		arr = new JSONArray(response.getBody());
		retweetCount = arr.getJSONObject(0).getString("retweet_count");

		then:
		retweetCount == "1";

		cleanup:
		TweeterAPI.sendStatusDestroyRequest(statusId);

	}
}