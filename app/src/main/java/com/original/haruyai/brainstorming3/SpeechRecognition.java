package com.original.haruyai.brainstorming3;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gigamole.library.PulseView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

/**
 * Created by haruya.i on 2017/10/29.
 */

public class SpeechRecognition extends AppCompatActivity  {
    String url;

    static final int RESULT_SPEECH = 1;  // The request code
    private SpeechRecognizer sr;

    private AudioManager mAudioManager;
    private int mStreamVolume = 0;

    //TextView textView;
    TextView hintview;

    //時間計測用
    Chronometer chronometer;
    long timeWhenStopped = 0;

    String hint = "";
    String resultsString = "";

    //会議終了ボタンのためにsummaryは初期化しとく
    private String[] summary_list = {"nothing", "", ""};
    private String[] keywords_list = new String[3];
    private String summary = "";
    private String keywords = "";

    ImageButton mic;
    boolean micFlag = false;

    String beforeListening = "マイクをタップすると録音が開始されます";
    String whileListening = "録音中です...";

    PulseView pulseView;

    MenuItem item;

    //会議ごとのIDを用意．遷移元から送られてくる
    private String newID ;
    //テーマを格納するところ
    private String theme = "";
    //参加者を格納するところ
    private String participants = "";
    //日付を格納するところ
    private String date = "";

    //会議終了したら押すやつ
    private Button finishButton;

    //[テーマ，参加者，日付，要約，アイデアマップの有無]．ストレージ保存用のオリジナル
    //finishButtonが押されたらストレージに保存して次回から閲覧できるようにするための配列
    private List<String> meetingDataArray = new ArrayList<>();

    //一時停止なのかそれともアイコン、会議終了ボタンを押したのか．trueがもう終わった時
    private boolean owattanoka = false;

    //会議ごとのIDを配列で．Gsonの処理がややこしいからString型にしてる
    private List<String> meetingID = new ArrayList<>();

    //
    boolean silentFrag = false;
    boolean hadconversationFrag = false;

    //ここでactionbar_menu.xmlをActionBar上に表示している
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu_speech, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        //このアクティビティオリジナル
        item = menu.findItem(R.id.to_summary);
        item.setVisible(false);

        return true;
    }

    //ActionBar上のアイコン（Item）が押されたことを検知するための関数
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //IDごとにボタンを押した際の処理を記述
        switch (item.getItemId()) {
            //戻るアイコンが押された時
            case android.R.id.home:
                finish();
                return true;

            //アイデアマップアイコンが押された時
            case R.id.node_icon:
                Intent intent_toIMap = new Intent(getApplication(), MainActivity.class);
                //アイデアマップにIDを教える．SpeechRecogtinionから行くのは新規作成の時だけだからnewIDでいい
                intent_toIMap.putExtra("newID", newID);

                startActivity(intent_toIMap);

                break;

            //議事録アイコンが押された時
            case R.id.to_summary:
                stopListening();

                //マイクの一時停止ではないことを示す
                owattanoka = true;

                url = "http://fullfill.sakura.ne.jp/JPHACKS/summary.php";

                //サーバとのやりとりの部分
                SendVoice job = new SendVoice();
                //サーバーの返信を待つ
                try{
                    Object result = job.execute().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                if(summary_list[0].equals("null")){
                    summary = "文章が短すぎるため要約できませんでした";
                }if(keywords_list[0].equals("null")){
                    keywords = "文章が短すぎるためキーワードを抽出できませんでした";
                }else{
                    for(int i=0; i<3; i++){
                        summary += summary_list[i] + " \n";
                        keywords += keywords_list[i] + " ";
                    }
                }

                //null消せるか
                summary = summary.replaceAll("null", "");
                keywords = keywords.replaceAll("null", "");

                //ストップボタンが押されると議事録画面に遷移する
                Intent intent_toSummary = new Intent(getApplication(), Summary.class);
                //変数(全ノードのリスト)を渡すとこ
                intent_toSummary.putExtra("minutes", resultsString);
                intent_toSummary.putExtra("summary", summary);
                intent_toSummary.putExtra("keywords", keywords);
                intent_toSummary.putExtra("participants", participants);
                intent_toSummary.putExtra("theme", theme);
                intent_toSummary.putExtra("date", date);
                startActivity(intent_toSummary);

                break;
        }
        return true;
    }

    //Activityが一時停止した場合
    @Override
    protected void onPause() {
        stopListening();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speeching_layout);

        //STARTボタンが押されるまでは音声認識を止めておく
        stopListening();

        // 遷移元(DataInputActivity)からID，テーマ，参加者，日付の文字列と会議IDのArrayListを受け取る
        Intent intent_from_DataInputActivity = getIntent();
        newID = intent_from_DataInputActivity.getStringExtra("newID");

        theme = intent_from_DataInputActivity.getStringExtra("theme");
        participants = intent_from_DataInputActivity.getStringExtra("participants");
        date = intent_from_DataInputActivity.getStringExtra("date");
        meetingID = intent_from_DataInputActivity.getStringArrayListExtra("meetingID");

        //textView = (TextView) this.findViewById(R.id.summary);
        hintview = (TextView)this.findViewById(R.id.hint);
        mic = (ImageButton)this.findViewById(R.id.mic);
        chronometer = (Chronometer)findViewById(R.id.recording_time);
        finishButton = (Button)findViewById(R.id.finishButton);

        hintview.setText(beforeListening);
        mic.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){
            ClickedpNpButton();}});

        pulseView = (PulseView)findViewById(R.id.pv);

        // ActionBarの設定
        if (savedInstanceState == null) {
            // customActionBarの取得
            View customActionBarView = this.getActionBarView("Hoge", "表示する画像のURL");
            // ActionBarの取得
            android.support.v7.app.ActionBar actionBar = this.getSupportActionBar();
            // 戻るボタンを表示するかどうか('<' <- こんなやつ)
            actionBar.setDisplayHomeAsUpEnabled(true);
            // タイトルを表示するか（もちろん表示しない）
            actionBar.setDisplayShowTitleEnabled(false);
            // iconを表示するか（もちろん表示しない）
            actionBar.setDisplayShowHomeEnabled(false);
            // ActionBarにcustomViewを設定する
            actionBar.setCustomView(customActionBarView);
            // CustomViewを表示するか
            actionBar.setDisplayShowCustomEnabled(true);
        }

        //会議終了ボタンのリスナー設置と押されてからの動作-------------------------------------------------
        finishButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //とりあえず録音を止める
                stopListening();

                url = "http://fullfill.sakura.ne.jp/JPHACKS/summary.php";

                //サーバとのやりとりの部分
                SendVoice job = new SendVoice();
                //サーバーの返信を待つ
                try{
                    Object result = job.execute().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                //議事録アイコンで一回呼ばれてた時用
                summary = "";
                keywords = "";

                if(summary_list[0].equals("null")){
                    summary = "文章が短すぎるため要約できませんでした";
                }if(keywords_list[0].equals("null")){
                    keywords = "文章が短すぎるためキーワードを抽出できませんでした";
                }else{
                    for(int i=0; i<3; i++){
                        summary += summary_list[i] + " \n";
                        keywords += keywords_list[i] + " ";
                    }
                }

                //null消せるか
                summary = summary.replaceAll("null", "");
                keywords = keywords.replaceAll("null", "");

                //sharedpreferenesの呼び出し
                Gson gson = new Gson();
                SharedPreferences sharedPreferences_shared = getSharedPreferences("shared" + newID, Context.MODE_PRIVATE);

                //[テーマ，参加者，日付，要約，アイデアマップの有無]
                meetingDataArray.add(theme);
                meetingDataArray.add(participants);
                meetingDataArray.add(date);
                meetingDataArray.add(summary);
                meetingDataArray.add(sharedPreferences_shared.getString("mapped_or_not" + newID,"0"));
                //ストレージに保存
                SharedPreferences.Editor editor_shared = sharedPreferences_shared.edit();
                //リスト表示用. [テーマ，参加者，日付，要約，アイデアマップの有無]
                editor_shared.putString("meetingDataArray" + newID, gson.toJson(meetingDataArray));

                //議事録系のデータの保存．(summaryが上のとちょっとカブってるから要注意)
                editor_shared.putString("minutes" + newID, resultsString);
                editor_shared.putString("summary" + newID, summary);
                editor_shared.putString("keywords" + newID, keywords);
                editor_shared.putString("theme" + newID, theme);
                editor_shared.putString("date" + newID, date);

                editor_shared.apply();

                //newIDを追加して保存し直す
                //最初の"1"がかぶるのを防ぐ
                if(meetingID.indexOf(newID) == -1){
                    meetingID.add(newID);
                }
                //ストレージに保存
                SharedPreferences sharedPreferences_meeting = getSharedPreferences("meeting", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor_meeting = sharedPreferences_meeting.edit();
                editor_meeting.putString("meetingID", gson.toJson(meetingID));
                editor_meeting.apply();

                //ListActivityに遷移
                Intent intent_toLA = new Intent(getApplication(), ListActivity.class);
                startActivity(intent_toLA);
            }
        });
    }


    // 音声認識を開始する
    protected void startListening() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        try {
            if (sr == null) {
                sr = SpeechRecognizer.createSpeechRecognizer(this);
                if (!SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "音声認識が使えません",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                sr.setRecognitionListener(new Listener());
            }
            // インテントの作成
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            // 言語モデル指定
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
            sr.startListening(intent);
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "startListening()でエラーが起こりました",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // 音声認識を終了する
    protected void stopListening() {
        if (sr != null) sr.destroy();
        sr = null;
    }

    // 音声認識を再開する
    public void restartListeningService() {
        //ピコンっていう音がいちいちしたらうるさいから消しておく
        //356行目らへんにもピコン音に関するコードあり
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        stopListening();
        startListening();
    }

    //音声認識して取得した文字列からHTTPでキーワードを抽出する関数
    class SendVoice extends AsyncTask<Integer, Integer, String> {
        @Override
        protected String doInBackground(Integer[] params) {
            try {
                hint = "";

                HttpClient httpclient = new DefaultHttpClient(null);
                HttpPost httppost = new HttpPost(url);

                // Request parameters and other properties.
                List<NameValuePair> param = new ArrayList<>(1);

                param.add(new BasicNameValuePair("sentence", resultsString));
                httppost.setEntity(new UrlEncodedFormEntity(param, "UTF-8"));

                //Execute and get the response.
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                JSONObject jsonObject = new JSONObject(EntityUtils.toString(entity));
                //STOPボタンが押された時
                if(owattanoka == true){
                    System.out.println(resultsString);
                    JSONArray jsonArray = jsonObject.getJSONArray("summary");
                    JSONArray jsonArray2 = jsonObject.getJSONArray("keywords");
                    for(int i=0; i<3; i++){
                        summary_list[i] = jsonArray.get(i).toString();
                        keywords_list[i] = jsonArray2.get(i).toString();
                    }
                }
                //まだ音声認識中（一時停止を含めて）の時
                else{
                    JSONArray jsonArray = jsonObject.getJSONArray("relation_keyword");
                    for(int i=0; i<3; i++){
                        hint = hint + " " + jsonArray.get(i).toString();
                    }
                    if(!jsonArray.get(0).toString().equals("null") || !jsonArray.get(0).toString().equals(null) || jsonArray.get(0)!=null){
                        //hint += "\nといったワードが面白そうです";
                    }else{
                        hint = whileListening;
                    }

                }
            } catch (Exception e) {
                Log.e("log_tag", "Error converting result " + e.toString());
            }
            return "test";
        }
    }


    // RecognitionListenerの定義
    // 中が空でも全てのメソッドを書く必要がある
    class Listener implements RecognitionListener {
        SpeechRecognition speechRecognition = new SpeechRecognition();

        // 話し始めたときに呼ばれる
        public void onBeginningOfSpeech() {
            /*Toast.makeText(getApplicationContext(), "onBeginningofSpeech",
                    Toast.LENGTH_SHORT).show();*/
        }
        public void onRmsChanged(float f){

        }

        // 結果に対する反応などで追加の音声が来たとき呼ばれる
        // しかし呼ばれる保証はないらしい
        public void onBufferReceived(byte[] buffer) {
        }

        // 話し終わった時に呼ばれる
        public void onEndOfSpeech() {
            /*Toast.makeText(getApplicationContext(), "onEndofSpeech",
                    Toast.LENGTH_SHORT).show();*/
        }

        // ネットワークエラーか認識エラーが起きた時に呼ばれる
        public void onError(int error) {
            String reason = "";
            switch (error) {
                // Audio recording error
                case SpeechRecognizer.ERROR_AUDIO:
                    reason = "ERROR_AUDIO";
                    break;
                // Other client side errors
                case SpeechRecognizer.ERROR_CLIENT:
                    reason = "ERROR_CLIENT";
                    break;
                // Insufficient permissions
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    reason = "ERROR_INSUFFICIENT_PERMISSIONS";
                    break;
                // 	Other network related errors
                case SpeechRecognizer.ERROR_NETWORK:
                    reason = "ERROR_NETWORK";
                    /* ネットワーク接続をチェックする処理をここに入れる */
                    break;
                // Network operation timed out
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    reason = "ERROR_NETWORK_TIMEOUT";
                    break;
                // No recognition result matched
                case SpeechRecognizer.ERROR_NO_MATCH:
                    reason = "ERROR_NO_MATCH";
                    break;
                // RecognitionService busy
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    reason = "ERROR_RECOGNIZER_BUSY";
                    break;
                // Server sends error status
                case SpeechRecognizer.ERROR_SERVER:
                    reason = "ERROR_SERVER";
                    /* ネットワーク接続をチェックをする処理をここに入れる */
                    break;
                // No speech input
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    reason = "ERROR_SPEECH_TIMEOUT";
                    if(silentFrag==true && hadconversationFrag==true){
                        //prompt();
                    }
                    silentFrag = true;
                    break;
            }
            //Toast.makeText(getApplicationContext(), reason, Toast.LENGTH_SHORT).show();
            restartListeningService();
        }
        // 将来の使用のために予約されている
        public void onEvent(int eventType, Bundle params) {
        }

        // 部分的な認識結果が利用出来るときに呼ばれる
        // 利用するにはインテントでEXTRA_PARTIAL_RESULTSを指定する必要がある
        public void onPartialResults(Bundle partialResults) {
        }

        // 音声認識の準備ができた時に呼ばれる
        public void onReadyForSpeech(Bundle params) {
            //Toast.makeText(getApplicationContext(), "話してください",
            //      Toast.LENGTH_SHORT).show();
        }

        // 認識結果が準備できた時に呼ばれる
        public void onResults(Bundle results) {
            // 結果をArrayListとして取得
            ArrayList results_array = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            // 取得した文字列を結合
            for (int i=0; i<1; i++) {
                resultsString += results_array.get(i) + "\n";
            }

            hadconversationFrag = true;

            url = "http://fullfill.sakura.ne.jp/JPHACKS/relation_char.php";

            SendVoice job = new SendVoice();
            job.execute();

            //アイデアのヒントとして、議事録から抽出したキーワードを定時する
            hintview.setVisibility(View.VISIBLE);
            hintview.setText(hint);

            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mStreamVolume, 0);

            restartListeningService();
        }
    }

    private void ClickedpNpButton(){
        //マイクをオンにした時
        if(micFlag == false){
            hintview.setText(whileListening);
            //stopClickedorNot = false;
            micFlag = true;
            startListening();
            //PulseViewをアクティヴにする（試験的）
            pulseView.startPulse();
            //議事録アイコンと会議終了ボタンを非表示にする → 遷移してバグを起こさないように
            item.setVisible(false);
            finishButton.setVisibility(View.INVISIBLE);

            //時間計測用
            chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
            chronometer.start();
        }
        //マイクをオフにした時
        else if(micFlag== true){
            stopListening();

            //サーバとのやりとりの部分
            SendVoice job = new SendVoice();
            job.execute();

            hintview.setText(beforeListening);

            micFlag = false;
            //PulseViewを止める
            pulseView.finishPulse();
            //議事録アイコンと会議終了ボタンを表示する
            item.setVisible(true);
            finishButton.setVisibility(View.VISIBLE);

            //時間計測用
            timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
            chronometer.stop();
        }
    }

    private void prompt(){
        Toast.makeText(this, "会話がなかったので、アイデアマップを表示します", Toast.LENGTH_SHORT).show();

        /*☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-
        ここで遷移させてるから、この辺でキーワードなどを与えて
        ☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-☆-  */

        //テスト用
        String[] test_string = {"ひらめき", "カプチーノ", "アイデアソン", "日本人", "ジャイロ", "縄跳び", "プログラミング",
                                "パソコン", "ショートヘア", "警察", "リア・ディゾン"};

        List<String> list = Arrays.asList(test_string);
        Collections.shuffle(list);
        String hint = list.get(0);

        // スプラッシュ完了後に実行するActivityを指定します。
        Intent intent = new Intent(getApplication(), MainActivity.class);

        intent.putExtra("hint_from_SR", hint);

        startActivity(intent);
        SpeechRecognition.this.finish();
    }

    //ActionBarのアイコンとか表示用
    public View getActionBarView(String title, String imageURL) {
        // 表示するlayoutファイルの取得
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.custom_action_bar, null);

        // CustomViewにクリックイベントを登録する
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return view;

    }
}


