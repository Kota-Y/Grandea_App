package com.original.haruyai.brainstorming3;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.igalata.bubblepicker.BubblePickerListener;
import com.igalata.bubblepicker.model.PickerItem;
import com.igalata.bubblepicker.rendering.BubblePicker;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haruya.i on 2017/10/27.
 */

public class Mapping extends AppCompatActivity {
    private List<String> defaultWords_List = new ArrayList<>();
    private List<String> originalWords_List = new ArrayList<>();

    BubblePicker bubblePicker;

    int image = R.drawable.likesilk;

    int default_color = Color.parseColor("#ff647a");
    int original_color = Color.parseColor("#f0db6b");

    private String newID;
    //閲覧用のときの変数
    private String clickedID;
    private boolean viewMode;

    //ActionBar上のアイコン（Item）が押されたことを検知するための関数
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //IDごとにボタンを押した際の処理を記述
        switch (item.getItemId()) {
            //戻るアイコンが押された時
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mapping_layout);

        newID = getIntent().getStringExtra("newID");
        //閲覧用
        clickedID = getIntent().getStringExtra("clickedID");
        viewMode = getIntent().getBooleanExtra("viewMode", false);

        //閲覧モードの時
        if(viewMode == true){
            newID = clickedID;
        }
        //defaultWords_ListとoriginalWords_Listをロードする
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getSharedPreferences("shared" + newID, Context.MODE_PRIVATE);
        defaultWords_List = gson.fromJson(sharedPreferences.getString("defaultWords_List" + newID, null), new TypeToken<List>(){}.getType());
        originalWords_List = gson.fromJson(sharedPreferences.getString("originalWords_List" + newID, null), new TypeToken<List>(){}.getType());


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
            // CutomViewを表示するか
            actionBar.setDisplayShowCustomEnabled(true);
        }


        bubblePicker = (BubblePicker) findViewById(R.id.picker);
        ArrayList<PickerItem> listItems = new ArrayList<>();
        for (int i = 0; i < defaultWords_List.size(); i++) {
            if(defaultWords_List.get(i).equals(" ") || defaultWords_List.get(i).equals("") || defaultWords_List.get(i).equals(null) || defaultWords_List.get(i) == null){
                break;
            }
            PickerItem item = new PickerItem(defaultWords_List.get(i), default_color, Color.WHITE, getDrawable(image));
            listItems.add(item);
        }
        for (int j=0; j<originalWords_List.size(); j++) {
            if(originalWords_List.get(j).equals(" ") || originalWords_List.get(j).equals("") || originalWords_List.get(j).equals(null)|| originalWords_List.get(j) == null) {
                break;
            }
            PickerItem item = new PickerItem(originalWords_List.get(j), original_color, Color.WHITE, getDrawable(image));
            listItems.add(item);
        }

        bubblePicker.setItems(listItems);
        bubblePicker.setListener(new BubblePickerListener() {
            @Override
            public void onBubbleSelected(@NotNull PickerItem pickerItem) {
                //Toast.makeText(getApplicationContext(), "" + pickerItem.getTitle() + " selected", Toast.LENGTH_SHORT).show();
                //name[0] = "clicked";

                ArrayList<PickerItem> listItems = new ArrayList<>();
                for (int i = 0; i < defaultWords_List.size(); i++) {
                    if(defaultWords_List.get(i).equals(" ") || defaultWords_List.get(i).equals("") || defaultWords_List.get(i).equals(null)){
                        break;
                    }
                    PickerItem item = new PickerItem(defaultWords_List.get(i), default_color, Color.WHITE, getDrawable(image));
                    listItems.add(item);
                }
                for (int j = 0; j < originalWords_List.size(); j++) {
                    if(originalWords_List.get(j).equals(" ") || originalWords_List.get(j).equals("") || originalWords_List.get(j).equals(null)) {
                        break;
                    }
                    PickerItem item = new PickerItem(originalWords_List.get(j), original_color, Color.WHITE, getDrawable(image));
                    listItems.add(item);
                }
            }

            @Override
            public void onBubbleDeselected(@NotNull PickerItem pickerItem) {
                //Toast.makeText(getApplicationContext(), "" + pickerItem.getTitle() + " Deselected", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
