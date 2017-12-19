package com.example.sissi.vconftest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by Sissi on 11/21/2016.
 */
public class Client extends BaseActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client);

        Button bt = (Button) findViewById(R.id.join_conf);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ConfActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt(ConfActivity.START_TYPE, ConfActivity.START_TYPE_JOIN_CONF);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
}
