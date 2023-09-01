package com.atguigu.gulimall.thread;

public class ThreadTest {
    public static void main(String[] args) {
        Thread01 thread01 = new Thread01();
        thread01.start();
        System.out.println("end-------------------");
    }
    public static class Thread01 extends Thread{
        @Override
        public void run(){
            System.out.println("当前线程"+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("结果"+i);
        }
    }
}
