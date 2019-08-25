package com.example.iomodel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

public class WriteAndRead {
	public static String PATH = "D:\\sun\\snowflakeIdWorker.txt";
	private List<WriteResource> resources;
	
	public WriteAndRead(List<WriteResource> resources) {
		this.resources = resources;
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
	}

	public  void readNIO() throws Exception {
		FileInputStream fin = new FileInputStream(new File(PATH));
		FileChannel channel = fin.getChannel();
		int capacity = 1000;// 字节
		ByteBuffer bf = ByteBuffer.allocate(capacity);
		System.out.println("限制是：" + bf.limit() + ",容量是：" + bf.capacity() + " ,位置是：" + bf.position());
		int length = -1;
		while ((length = channel.read(bf)) != -1) {

			/*
			 * 注意，读取后，将位置置为0，将limit置为容量, 以备下次读入到字节缓冲中，从0开始存储
			 */
			bf.clear();
			byte[] bytes = bf.array();
			System.out.println("start..............");

			String str = new String(bytes, 0, length);
			System.out.println(str);
			// System.out.write(bytes, 0, length);

			System.out.println("end................");

			System.out.println("限制是：" + bf.limit() + "容量是：" + bf.capacity() + "位置是：" + bf.position());

		}

		channel.close();
		fin.close();
	}

	public  void writeNIO() throws Exception {
		if(resources == null){
			throw new Exception("没有结果集");
		}
		FileOutputStream fos = new FileOutputStream(new File(PATH));
		FileChannel channel = fos.getChannel();
		for(WriteResource resource : resources){
			ByteBuffer src = Charset.forName("utf8").encode(resource.WriteResource().toString());
			// 字节缓冲的容量和limit会随着数据长度变化，不是固定不变的
			System.out.println("初始化容量和limit：" + src.capacity() + "," + src.limit());
			int length = 0;
			while ((length = channel.write(src)) != 0) {
				/*
				 * 注意，这里不需要clear，将缓冲中的数据写入到通道中后 第二次接着上一次的顺序往下读
				 */
				System.out.println("写入长度:" + length);
			}
		}
		fos.close();
	}

	public  void testReadAndWriteNIO() {
		FileInputStream fin = null;
		FileOutputStream fos = null;
		try {
			fin = new FileInputStream(new File(PATH));
			FileChannel channel = fin.getChannel();

			int capacity = 100;// 字节
			ByteBuffer bf = ByteBuffer.allocate(capacity);
			System.out.println("限制是：" + bf.limit() + "容量是：" + bf.capacity() + "位置是：" + bf.position());
			int length = -1;

			fos = new FileOutputStream(new File(PATH));
			FileChannel outchannel = fos.getChannel();

			while ((length = channel.read(bf)) != -1) {

				// 将当前位置置为limit，然后设置当前位置为0，也就是从0到limit这块，都写入到同道中
				bf.flip();

				int outlength = 0;
				while ((outlength = outchannel.write(bf)) != 0) {
					System.out.println("读，" + length + "写," + outlength);
				}

				// 将当前位置置为0，然后设置limit为容量，也就是从0到limit（容量）这块，
				// 都可以利用，通道读取的数据存储到
				// 0到limit这块
				bf.clear();

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
