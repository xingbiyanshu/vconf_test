package com.example.sissi.vconftest;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class ConfActivity extends BaseActivity {
    public static final String START_TYPE = "START_TYPE";
    public static final int START_TYPE_JOIN_CONF = 1;

    private BaseFragment prepareConfFrag;   // 创会过程中的画面（呼叫、入会）
    private BaseFragment confBigMainFrag;   // 会议中的主大画面（对端图像播放、预览）
    private BaseFragment confBigAssFrag;    // 会议中的辅大画面（主动共享文档画面、双流缓冲画面、静态图片画面）
    private BaseFragment confSmallMainFrag; // 会议中的主小画面（预览、对端图像播放）
    private BaseFragment confSmallAssFrag;  // 会议中的辅小画面（主动共享文档画面、静态图片画面）
    private BaseFragment topFrag;           // 顶部栏
    private BaseFragment bottomFrag;        // 底部栏
    private BaseFragment tmpFrag;

    private ViewGroup mainView;
    private ViewGroup assView;
    private ViewGroup tmpView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conf_main);

        mainView = (ViewGroup) findViewById(R.id.first_stream_frame);
        assView = (ViewGroup) findViewById(R.id.preview_frame);
        tmpView = (ViewGroup) findViewById(R.id.tmp_frame);

        initViews();

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        int type = START_TYPE_JOIN_CONF;
        if (null != bundle) {
            type = bundle.getInt(START_TYPE, START_TYPE_JOIN_CONF);
        }
        switchStartFrag(type);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void switchStartFrag(int type){
        PcTrace.p("=>");
        switch (type){
            case START_TYPE_JOIN_CONF:
                setFragment(R.id.prepare_frame, new JoinConfFrag());
                break;
            default:
                break;
        }
    }

    private void setFragment(int containerId, BaseFragment frag){
        PcTrace.p("=> container="+ containerId +" frag="+frag);
        switch (containerId){
            case R.id.prepare_frame:
                prepareConfFrag = frag;
                break;
            case R.id.first_stream_frame:
                confBigMainFrag = frag;
                break;
//            case R.id.big_ass_frame:
//                confBigAssFrag = frag;
//                break;
            case R.id.preview_frame:
                confSmallMainFrag = frag;
                break;
//            case R.id.small_ass_frame:
//                confSmallAssFrag = frag;
//                break;
            case R.id.tmp_frame:
                tmpFrag = frag;
                break;
            case R.id.top_frame:
                topFrag = frag;
                break;
            case R.id.bottom_frame:
                bottomFrag = frag;
                break;
            default:
                return;
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(containerId, frag);
        ft.commitAllowingStateLoss();
    }

    private void removeFragment(BaseFragment frag){
        PcTrace.p("=>");
        if (frag == prepareConfFrag){
            prepareConfFrag = null;
        }else if (frag == confBigMainFrag){
            confBigMainFrag = null;
        }else if (frag == confBigAssFrag){
            confBigAssFrag = null;
        }else if (frag == confSmallMainFrag){
            confSmallMainFrag = null;
        }else if (frag == confSmallAssFrag){
            confSmallAssFrag = null;
        }else if (frag == tmpFrag){
            tmpFrag = null;
        }else if (frag == topFrag){
            topFrag = null;
        }else if (frag == bottomFrag){
            bottomFrag = null;
        }else{
            return;
        }

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(frag);
        ft.commitAllowingStateLoss();
    }


    @Override
    public void fragChanged(/*BaseFragment.Event*/FragEvent id, Object content) {
        PcTrace.p("=>");
        switch (id){
            case VConf_JoinConf_Succeed:
                removeFragment(prepareConfFrag);
                findViewById(R.id.prepare_frame).setVisibility(View.GONE);

//                findViewById(R.id.conf_frame).setVisibility(View.VISIBLE);
                setFragment(R.id.first_stream_frame, new StreamRecvFrag());
                setFragment(R.id.preview_frame, new FaceViewFrag());
                setFragment(R.id.top_frame, new TopFuncFrag());
                setFragment(R.id.bottom_frame, new BottomFuncFrag());
                break;

            case VConf_BottomFuncBar_Switch:
                exchangeMainAssView();
                break;

            case VConf_BottomFuncBar_Open_ShareDoc:
                openShare();
                break;

            case VConf_BottomFuncBar_Close_ShareDoc:
                closeShare();
                break;

            default:
                break;
        }
    }

    /*切换大小画面*/
    private void exchangeMainAssView(){
        PcTrace.p("=>");
        //途径1:切frag的container
/*        ViewGroup bigVg = (ViewGroup) findViewById(R.id.big_main_frame);
        ViewGroup smallVg = (ViewGroup) findViewById(R.id.small_main_frame);

        bigVg.removeView(confBigMainFrag.getView());
        smallVg.removeView(confSmallMainFrag.getView());
        bigVg.addView(confSmallMainFrag.getView());
        smallVg.addView(confBigMainFrag.getView());

        BaseFragment tmp = confBigMainFrag;
        confBigMainFrag = confSmallMainFrag;
        confSmallMainFrag = tmp;*/

        //途径2:切fragment
/*        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(confBigMainFrag);
        ft.remove(confSmallMainFrag);
        ft.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();

        FragmentTransaction ft2 = getFragmentManager().beginTransaction();
        ft2.add(R.id.big_main_frame, confSmallMainFrag);
        ft2.add(R.id.small_main_frame, confBigMainFrag);
        ft2.commitAllowingStateLoss();

        BaseFragment tmp = confBigMainFrag;
        confBigMainFrag = confSmallMainFrag;
        confSmallMainFrag = tmp;*/

        // 途径3:切framelayout
        ViewGroup.LayoutParams params1 = mainView.getLayoutParams();
        ViewGroup.LayoutParams params2 = assView.getLayoutParams();
        ViewGroup tmp = mainView;
        mainView = assView;
        assView = tmp;
        mainView.setLayoutParams(params1);
        assView.setLayoutParams(params2);
    }

    private void exchangeMainTmpView(){
        ViewGroup.LayoutParams params1 = mainView.getLayoutParams();
        ViewGroup.LayoutParams params2 = tmpView.getLayoutParams();
        ViewGroup tmp = mainView;
        mainView = tmpView;
        tmpView = tmp;
        mainView.setLayoutParams(params1);
        tmpView.setLayoutParams(params2);

        mainView.setVisibility(View.VISIBLE);
        tmpView.setVisibility(View.GONE);
    }

    private void openShare(){
        PcTrace.p("=>");
        setFragment(R.id.tmp_frame, new ShareDocFrag());
//        tmpView.setLayoutParams(mainView.getLayoutParams());
//        tmpView.setVisibility(View.VISIBLE);
//        mainView.setVisibility(View.GONE);
//        ViewGroup tmp = mainView;
//        mainView = tmpView;
//        tmpView = tmp;
        if (mainView.getId() == R.id.preview_frame){
            exchangeMainAssView();
        }
        exchangeMainTmpView();
    }

    private void closeShare(){
        PcTrace.p("=>");
        if (mainView.getId() == R.id.preview_frame){
            exchangeMainAssView();
        }
        exchangeMainTmpView();
        removeFragment(tmpFrag);
    }

    private void initViews(){
    }

}
