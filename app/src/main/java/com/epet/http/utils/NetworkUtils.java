package com.epet.http.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 网络监控辅助工具类
 * 作者：yuer on 2017/8/18 18:39
 * 邮箱：heziyu222@163.com
 */

public class NetworkUtils {

    //NetworkAvailable-网络连接可用
    private static final int NET_CONNECT_OK = 0;

    //No Network ready
    private static final int NET_NO_PREPARE = 1;

    //网络错误
    private static final int NET_ERROR = 2;

    /**
     * 检查网络是否可用
     * @param context
     * @return true为可用  false为不可用
     */
    public static boolean isNetWorkAvailable(Context context){
        boolean isAvailable = true;
        int state = networkState(context);
        if(NET_CONNECT_OK!=state)
            isAvailable = false;
        return isAvailable;
    }

    /**
     * 当前网络状态
     * @param context
     * @return
     */
    public static int networkState(Context context){
        ConnectivityManager netMananger = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = netMananger.getActiveNetworkInfo();
        if(networkInfo != null){
            if(networkInfo.isAvailable() && networkInfo.isConnected()){
                return NET_CONNECT_OK;
            }else{
                return NET_NO_PREPARE;
            }
        }
        return NET_ERROR;
    }

    /**
     * 获取本地ip地址
     * @return
     */
    public static String getLocationIpAddress(){
        String ip = "";
        try {
            for (Enumeration<NetworkInterface> enume =  NetworkInterface.getNetworkInterfaces() ; enume.hasMoreElements();) {
                NetworkInterface netInterface = enume.nextElement();
                for (Enumeration<InetAddress> inetAddress =  netInterface.getInetAddresses();inetAddress.hasMoreElements();){
                    InetAddress netAddress = inetAddress.nextElement();
                    if(!netAddress.isLoopbackAddress()){
                        ip = netAddress.getHostAddress().toString();
                    }
                }
            }
        }catch (SocketException ex){
            ex.printStackTrace();
        }
        return ip;
    }

    /**
     * 当前网络是否为3G网络
     * @param context
     * @return true 为3G
     */
    public static boolean is3G(Context context){
        ConnectivityManager netManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = netManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE){
            return true;
        }
        return false;
    }

    /**
     * 是否为wifi
     * @param context
     * @return true 为wifi
     */
    public static boolean isWifi(Context context){
        ConnectivityManager netManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = netManager.getActiveNetworkInfo();
        if(networkInfo != null && networkInfo.getType()==ConnectivityManager.TYPE_WIFI){
            return true;
        }
        return false;
    }

    /**
     * 当前网络是否为2G
     * @param context
     * @return true 为2G
     */
    public static boolean is2G(Context context){
        ConnectivityManager netManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = netManager.getActiveNetworkInfo();
        if(networkInfo != null && (networkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE
                || networkInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS || networkInfo
                .getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA)){
            return true;
        }
        return false;
    }


    /**
     * 当前wifi是否可用
     * @param context
     * @return
     */
    public static boolean isWifiEnable(Context context){
        ConnectivityManager netManager = (ConnectivityManager)context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager telManager = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        NetworkInfo networkInfo = netManager.getActiveNetworkInfo();
        if((networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) || telManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS ){
            return true;
        }
        return false;
    }
}
