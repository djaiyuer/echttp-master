package com.epet.http.engine;

import android.text.TextUtils;
import android.util.Log;

import com.epet.http.bean.DownInfoBean;
import com.epet.http.imple.HttpEngineImple;
import com.epet.http.listener.IHttpService;
import com.epet.http.listener.OnResultListener;
import com.epet.http.listener.ProgressListener;
import com.epet.http.body.ProgressRequestBody;
import com.epet.http.observer.HttpDownLoadObServer;
import com.epet.http.observer.HttpGeneralObServer;
import com.epet.http.utils.Applications;
import com.epet.http.utils.FileUtil;
import com.epet.http.utils.okhttps.OkHttpClientUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Retrofit 网络框架
 * 作者：yuer on 2017/8/17 13:58
 * 邮箱：heziyu222@163.com
 */

public class RetrofitHttpEngine extends HttpEngineImple {
    private IHttpService mIhttpService;

    public RetrofitHttpEngine(Builder builder) {
        super(builder);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(builder.getBaseUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(OkHttpClientUtils.newInstance().createOkHttpCilentBuilder(builder))
                .build();
        this.mIhttpService = retrofit.create(IHttpService.class);
    }

    @Override
    public void httpGet() {
        request(this.mIhttpService.requestGet(getUrl(), this.mBuidler.getParam()));
    }

    @Override
    public void httpPost() {
        request(this.mIhttpService.requestPost(getUrl(), this.mBuidler.getParam()));
    }
    @Override
    public void httpPut() {
        request( this.mIhttpService.requestPut(getUrl(), this.mBuidler.getParam()));
    }

    @Override
    public void httpDelete() {
        request( this.mIhttpService.requestDelete(getUrl(), this.mBuidler.getParam()));
    }

    @Override
    public void upload() {
        super.upload();
        request( this.mIhttpService.upload(getUrl(),filesToMultipartBodyParts()));
    }
    @Override
    public void download(){
        HttpDownLoadObServer observer = new HttpDownLoadObServer(this.mBuidler);
        Observable<ResponseBody> observable = this.mIhttpService.download(this.mBuidler.getUrl());
        observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .map(new Function<ResponseBody, DownInfoBean>() {
                    @Override
                    public DownInfoBean apply(ResponseBody responseBody) {
                        DownInfoBean info = mBuidler.getDownLoadInfo();
                        try {
                            String filePath = info.getSavePath();
                            String fileName = info.getSaveFileName();
                            if(TextUtils.isEmpty(filePath)){
                                filePath = FileUtil.getDefaultDownLoadPath(Applications.context());
                                info.setSavePath(filePath);
                            }
                            if(TextUtils.isEmpty(fileName)){
                                fileName = FileUtil.getDefaultDownLoadFileName(mBuidler.getUrl());
                                info.setSaveFileName(fileName);
                            }
                            writeCache(responseBody,new File(filePath,fileName),info);
                        } catch (Exception e) {
                            /*失败抛出异常*/
                            e.printStackTrace();
                        }
                        return info;
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);

    }

    /**
     * 获取url
     * @return
     */
    private String getUrl(){
       return this.mBuidler.getUrl();
    }

    /**
     * 请求服务器
     * @param observable
     */
    private void request(Observable<ResponseBody> observable) {
        observable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new HttpGeneralObServer(mBuidler));
    }

    /**
     * 组装多文件Multipart
     * @return
     */
    public  List<MultipartBody.Part> filesToMultipartBodyParts() {
        ArrayList<File> files = mBuidler.getFiles();
        final OnResultListener listener = mBuidler.getListener();
        if(files==null || files.isEmpty()){
            return new ArrayList<>();
        }
        List<MultipartBody.Part> parts = new ArrayList<>(files.size());
        for (File file : files) {
            RequestBody requestBody=RequestBody.create(MediaType.parse("multipart/form-data"),file);
            MultipartBody.Part filePart= MultipartBody.Part.createFormData(this.mBuidler.getFileKey(), file.getName(), new ProgressRequestBody(requestBody,file.getPath(),
                    new ProgressListener() {
                        @Override
                        public void onProgress(String filePath,long currentBytesCount, long totalBytesCount,boolean isFinish) {
                            Log.d(RetrofitHttpEngine.class.getSimpleName(),"filePath:"+filePath+",totalBytesCount:"+totalBytesCount+",currentBytesCount:"+currentBytesCount);
                            if (listener != null ) {
                                if(isFinish){
                                    listener.uploadComplate(filePath);
                                }else{
                                    listener.uploadProgress(filePath,currentBytesCount,totalBytesCount);
                                }
                            }
                        }
                    }));
            parts.add(filePart);
        }
        return parts;
    }

    /**
     * 写入文件
     * @param file
     * @param info
     * @throws IOException
     */
    public void writeCache(ResponseBody responseBody, File file, DownInfoBean info) {
        try {
            RandomAccessFile randomAccessFile = null;
            FileChannel channelOut = null;
            InputStream inputStream = null;
            try {
                if (!file.getParentFile().exists()){
                    file.getParentFile().mkdirs();
                }
                long allLength = 0 == info.getTotalLength() ? responseBody.contentLength() : info.getReadLength() + responseBody
                        .contentLength();
                inputStream = responseBody.byteStream();
                randomAccessFile = new RandomAccessFile(file, "rwd");
                channelOut = randomAccessFile.getChannel();
                MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE,
                        info.getReadLength(), allLength - info.getReadLength());
                byte[] buffer = new byte[1024 * 4];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    mappedBuffer.put(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (channelOut != null) {
                    channelOut.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
