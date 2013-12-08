package org.ralit.ofutonreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import android.net.http.AndroidHttpClient;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class MojiRecognize extends Thread {
	
	/**
	 * 使用ライブラリ
	 * httpclient-4.2.3
	 * httpcore-4.2.3
	 * httpmime-4.2.3
	 * jackson-annotations-2.2.0
	 * jackson-core-2.2.0
	 * jackson-databind-2.2.0
	 */
	
	private String mFilePath;
	
	public MojiRecognize(String filePath) {
		mFilePath = filePath;
	}
	
	public void run() {
		try {
			// HTTPClientはどっちかを使う
			// 1. org.apache.http.impl.client.DefaultHttpClient
			// 2. android.net.http.AndroidHttpClient
//			DefaultHttpClient client = new DefaultHttpClient(); // (1) こっちも動きました
			AndroidHttpClient client = AndroidHttpClient.newInstance("Android UserAgent"); // (2)
			HttpPost post = new HttpPost("https://api.apigw.smt.docomo.ne.jp/characterRecognition/v1/scene?APIKEY=" + DocomoAPI.getApi());
			// これを知らなかった。MultipartのPOSTをするときはこのクラスを使おう。
			MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			FileBody fileBody = new FileBody(new File(mFilePath), "image/jpeg");
			multipartEntity.addPart("image", fileBody);
			post.setEntity(multipartEntity);
			// 通信開始
			HttpResponse response = client.execute(post);
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line + "\n");
			}
			// JSONパース(jobIDの取得)
			JsonNode jsonNode = new ObjectMapper().readTree(builder.toString());
			String jobID = jsonNode.path("job").path("@id").asText();
			// 認識結果取得待ち
			int waitingTime = 0;
			while(true) {
				Thread.sleep(1000);
				HttpGet get = new HttpGet("https://api.apigw.smt.docomo.ne.jp/characterRecognition/v1/scene/" + jobID + "?APIKEY=" + DocomoAPI.getApi());
				get.setHeader("Content-Type", "application/json");
				response = client.execute(get);
				// HttpResponseのEntityデータをStringへ変換
				reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				builder = new StringBuilder();
				line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line + "\n");
				}
				jsonNode = new ObjectMapper().readTree(builder.toString());
				String status = jsonNode.path("job").path("@status").asText();
				if (!status.equals("process") && !status.equals("queue")) { break; }
				waitingTime++;
				log("status: " + status + " (" + waitingTime + " sec)");
			}
			// JSONパース(
			JsonNode wordNode = null;
			ArrayList<Word> wordList = new ArrayList<Word>();
			log(jsonNode.toString());
			for (int i = 0; (wordNode = jsonNode.path("words").path("word").get(i)) != null; i++) {
				Word word = new Word();
				JsonNode pointNode = wordNode.path("shape").path("point");
//				log(pointNode.toString());
				word.setPoint(pointNode.get(0).path("@x").asInt(), pointNode.get(0).path("@y").asInt(), pointNode.get(2).path("@x").asInt(), pointNode.get(2).path("@y").asInt());
//				log(String.valueOf(word.getArea()));
				word.setText(wordNode.path("@text").asText());
				word.setScore(wordNode.path("@score").asInt());
				wordList.add(word);
			}

			

		} catch (Exception e) {
			e.getStackTrace();
		}

	}


	private void log(String log) {
		Log.i("ralit", log);
	}
}
