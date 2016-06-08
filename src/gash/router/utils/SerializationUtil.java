/**
 * 
 */
package gash.router.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.google.protobuf.ByteString;

/**
 
 * @author bg
 * 
 */
public class SerializationUtil {

	public List<ByteString> readfile(String file, long from, int size, int N) {

		List<ByteString> output = new LinkedList<>();
		BufferedInputStream bis = null;
		try {
			
			// buffer to read file
			byte[] data = new byte[size];
			bis = new BufferedInputStream(new FileInputStream(file));
			
			// Skip file to the chunk to read from
			bis.skip(from * size);
			int index = 0;
			
			// Read file
			while (bis.read(data) > 0 && (N == -1 || index++ < N)) {
				output.add(ByteString.copyFrom(data));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return output;
	}


	public void writeFile(String file, List<ByteString> input) {
		BufferedOutputStream bos = null;
		FileOutputStream fos=null;
		try {
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			System.out.println( "noof data chunks in file"+input.size());
			for (ByteString data : input) {
				bos.write(data.toByteArray());
				//bos.flush();
			}
			System.out.println("Write Complete");
			//fos.getFD().sync();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bos.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
