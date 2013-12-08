package org.ralit.ofutonreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MojiRecognize extends Thread implements OnInitListener{

	/**
	 * 使用ライブラリ
	 * httpclient-4.2.3
	 * httpcore-4.2.3
	 * httpmime-4.2.3
	 * jackson-annotations-2.2.0
	 * jackson-core-2.2.0
	 * jackson-databind-2.2.0
	 */

	private String filePath;
	private Context context;
	private boolean isSpeechInitialized = false;
	TextToSpeech textToSpeech;

	public MojiRecognize(String filePath, Context context) {
		this.filePath = filePath;
		this.context = context;
		textToSpeech = new TextToSpeech(context, this, "jp.kddilabs.n2tts");
	}

	private HttpResponse requestJobID() {
		try {
			// HTTPClientはどっちかを使う
			// 1. org.apache.http.impl.client.DefaultHttpClient
			// 2. android.net.http.AndroidHttpClient
			// DefaultHttpClient client = new DefaultHttpClient(); // (1) こっちも動きました
			AndroidHttpClient client = AndroidHttpClient.newInstance("Android UserAgent"); // (2)
			HttpPost post = new HttpPost("https://api.apigw.smt.docomo.ne.jp/characterRecognition/v1/scene?APIKEY=" + DocomoAPI.getApi());
			// これを知らなかった。MultipartのPOSTをするときはこのクラスを使おう。
			MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			FileBody fileBody = new FileBody(new File(filePath), "image/jpeg");
			multipartEntity.addPart("image", fileBody);
			post.setEntity(multipartEntity);
			// 通信開始
			HttpResponse response = client.execute(post);
			return response;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	private String parseJobID(HttpResponse response) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}
			JsonNode jsonNode = new ObjectMapper().readTree(builder.toString());
			String jobID = jsonNode.path("job").path("@id").asText();
			return jobID;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private JsonNode requestResult(String jobID) {
		try { 
			AndroidHttpClient client = AndroidHttpClient.newInstance("Android UserAgent");
			HttpGet get = new HttpGet("https://api.apigw.smt.docomo.ne.jp/characterRecognition/v1/scene/" + jobID + "?APIKEY=" + DocomoAPI.getApi());
			get.setHeader("Content-Type", "application/json");
			HttpResponse response = client.execute(get);
			// HttpResponseのEntityデータをStringへ変換
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}
			JsonNode jsonNode = new ObjectMapper().readTree(builder.toString());
			return jsonNode;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private ArrayList<Word> parseMoji(JsonNode jsonNode) {
		JsonNode wordNode = null;
		ArrayList<Word> wordList = new ArrayList<Word>();
		log(jsonNode.toString());
		for (int i = 0; (wordNode = jsonNode.path("words").path("word").get(i)) != null; i++) {
			Word word = new Word();
			JsonNode pointNode = wordNode.path("shape").path("point");
//			log(pointNode.toString());
			word.setPoint(pointNode.get(0).path("@x").asInt(), pointNode.get(0).path("@y").asInt(), pointNode.get(2).path("@x").asInt(), pointNode.get(2).path("@y").asInt());
//			log(String.valueOf(word.getArea()));
			word.setText(wordNode.path("@text").asText());
			word.setScore(wordNode.path("@score").asInt());
			wordList.add(word);
		}
		return wordList;
	}
	
	public void run() {
		try {
			// 画像を送ってjobIDをもらう
			HttpResponse response = requestJobID();
			// JSONパース(jobIDの取得)
			String jobID = parseJobID(response);
			// 認識結果取得待ち
			int waitingTime = 0;
			JsonNode jsonNode = null;
			while(true) {
				Thread.sleep(1000);
				// 認識結果をリクエスト
				jsonNode = requestResult(jobID);
				String status = jsonNode.path("job").path("@status").asText();
				if (!status.equals("process") && !status.equals("queue")) { break; }
				log("status: " + status + " (" + ++waitingTime + " sec)");
			}
			// 認識結果が返ってきた
			// JSONパース(文字情報)
			ArrayList<Word> wordList = parseMoji(jsonNode);
			Collections.sort(wordList, new PointComparator());
			log(String.valueOf(wordList.size()));
			
			// 読み上げ
			
			if (isSpeechInitialized) {
				log("isSpeechInitialized = true");
			} else {
				log("isSpeechInitialized = false");
			}
			for (int i = 0; i < wordList.size(); i++) {
				textToSpeech.speak(wordList.get(i).getText(), TextToSpeech.QUEUE_ADD, null);
			}
			
		} catch (Exception e) {
			e.getStackTrace();
		}
	}

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			isSpeechInitialized = true;
		}
	}

	private void log(String log) {
		Log.i("ralit", log);
	}
}
