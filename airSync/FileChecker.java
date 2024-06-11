package airSync;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileChecker {
    public static void main(String[] args) {
        // 定义目标文件名
        String targetFileName = "Deploy.txt";

        // 创建File对象
        File targetFile = new File(targetFileName);

        // 检查文件是否存在
        if (targetFile.exists()) {
            System.out.println("文件 " + targetFileName + " 存在，一切正常。");
        } else {
            try {
                // 创建文件
                if (targetFile.createNewFile()) {
                    System.out.println("文件 " + targetFileName + " 不存在，已创建新文件。");
                    // 写入一些初始内容到文件中（可选）
                    FileWriter writer = new FileWriter(targetFile);
                    writer.write("这是一个新创建的文件。");
                    writer.close();
                } else {
                    System.out.println("文件 " + targetFileName + " 无法创建。");
                }
            } catch (IOException e) {
                System.err.println("发生IO异常: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
