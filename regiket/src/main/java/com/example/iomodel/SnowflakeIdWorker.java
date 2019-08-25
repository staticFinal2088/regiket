package com.example.iomodel;

import java.util.concurrent.atomic.AtomicLong;
/**
 * 核心思想：

SnowFlake的结构如下(每部分用-分开):<br>
* 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000 <br>
* 1位标识，由于long基本类型在Java中是带符号的，最高位是符号位，正数是0，负数是1，所以id一般是正数，最高位是0<br>
* 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
* 得到的值），这里的的开始时间截，一般是我们的id生成器开始使用的时间，由我们程序来指定的（如下下面程序IdWorker类的startTime属性）。41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69<br>
* 10位的数据机器位，可以部署在1024个节点，包括5位datacenterId和5位workerId<br>
* 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
* 加起来刚好64位，为一个Long型。<br>
* SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由数据中心ID和机器ID作区分)，并且效率较高，经测试，SnowFlake每秒能够产生26万ID左右。
*


 网上的教程一般存在两个问题：
* 1. 机器ID（5位）和数据中心ID（5位）配置没有解决，分布式部署的时候会使用相同的配置，任然有ID重复的风险。
* 2. 使用的时候需要实例化对象，没有形成开箱即用的工具类。
*
* 本文针对上面两个问题进行解决，笔者的解决方案是，workId使用服务器hostName生成，
* dataCenterId使用IP生成，这样可以最大限度防止10位机器码重复，但是由于两个ID都不能超过32，
* 只能取余数，还是难免产生重复，但是实际使用中，hostName和IP的配置一般连续或相近，
* 只要不是刚好相隔32位，就不会有问题，况且，hostName和IP同时相隔32的情况更加是几乎不可能
* 的事，平时做的分布式部署，一般也不会超过10台容器。使用上面的方法可以零配置使用雪花算法，
* 雪花算法10位机器码的设定理论上可以有1024个节点，生产上使用docker配置一般是一次编译，
* 然后分布式部署到不同容器，不会有不同的配置，这里不知道其他公司是如何解决的，即使有方法
* 使用一套配置，然后运行时根据不同容器读取不同的配置，但是给每个容器编配ID，1024个
* （大部分情况下没有这么多），似乎也不太可能，此问题留待日后解决后再行补充。
*

 * @author Administrator
 *
 */
public class SnowflakeIdWorker {
	 /**
     * 每一部分所占位数
     */
    private final long unusedBits = 1L;
    private final long timestampBits = 41L;
    private final long datacenterIdBits = 5L;
    private final long workerIdBits = 5L;
    private final long sequenceBits = 12L;

    /**
     * 向左的位移
     */
    private final long timestampShift = sequenceBits + datacenterIdBits + workerIdBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long workerIdShift = sequenceBits;

    /**
     * 起始时间戳，初始化后不可修改
     */
    private final long epoch = 1451606400000L; // 2016-01-01

    /**
     * 数据中心编码，初始化后不可修改
     * 最大值: 2^5-1 取值范围: [0,31]
     */
    private final long datacenterId;

    /**
     * 机器或进程编码，初始化后不可修改
     * 最大值: 2^5-1 取值范围: [0,31]
     */
    private final long workerId;

    /**
     * 序列号
     * 最大值: 2^12-1 取值范围: [0,4095]
     */
    private long sequence = 0L;

    /** 上次执行生成 ID 方法的时间戳 */
    private long lastTimestamp = -1L;

    /*
     * 每一部分最大值
     */
    private final long MAX_DATACENTER_ID = -1L ^ (-1L << datacenterIdBits); // 2^5-1
    private final long MAX_WORKER_ID = -1L ^ (-1L << workerIdBits); // 2^5-1
    private final long maxSequence = -1L ^ (-1L << sequenceBits); // 2^12-1
    public static void main(String[] args) {
    	
    	for(int i = 0 ; i < 32 ; i++){
    		for(int j = 0 ; j < 32 ; j ++){
    			SnowflakeIdWorker snow = new SnowflakeIdWorker(i,j);
    			System.out.println(snow.nextId());
    		}
    	}
    	
	}
    /**
     * 生成序列号
     */
    public synchronized long nextId() {
        long currTimestamp = timestampGen();

        if (currTimestamp < lastTimestamp) {
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                            lastTimestamp - currTimestamp));
        }

        if (currTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & maxSequence;
            if (sequence == 0) { // overflow: greater than max sequence
                currTimestamp = waitNextMillis(currTimestamp);
            }

        } else { // reset to 0 for next period/millisecond
            sequence = 0L;
        }

        // track and memo the time stamp last snowflake ID generated
        lastTimestamp = currTimestamp;

        return ((currTimestamp - epoch) << timestampShift) | //
                (datacenterId << datacenterIdShift) | //
                (workerId << workerIdShift) | // new line for nice looking
                sequence;
    }
    /**
     * 构造函数
     *
     * @param workerId     工作ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeIdWorker(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }

        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 追踪调用 waitNextMillis 方法的次数
     */
    private final AtomicLong waitCount = new AtomicLong(0);

    public long getWaitCount() {
        return waitCount.get();
    }

    /**
     * 循环阻塞直到下一秒
     */
    protected long waitNextMillis(long currTimestamp) {
        waitCount.incrementAndGet();
        while (currTimestamp <= lastTimestamp) {
            currTimestamp = timestampGen();
        }
        return currTimestamp;
    }

    /**
     * 获取当前时间戳
     */
    public long timestampGen() {
        return System.currentTimeMillis();
    }
}
