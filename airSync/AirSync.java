package airSync;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AirSync {
    private static String mainDir;
    private static String syncDir;
    private static final String LOG_FILE = "operation_log.txt";

    public static void main(String[] args) {
        try {
            // 读取Deploy.txt文件
            readDeployFile();

            // 输出目录路径
            System.out.println("主文件夹路径: " + mainDir);
            System.out.println("同步文件夹路径: " + syncDir);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                // 选择操作
                System.out.println("请选择操作：1 - 查看日志，2 - 进行同步，3 - 退出");
                int choice = scanner.nextInt();
                scanner.nextLine(); // 清除缓冲区

                if (choice == 1) {
                    // 查看近期操作记录
                    printRecentOperations();
                    // 询问是否进行同步
                    if (askToProceed(scanner, "是否进行同步？(yes/no): ")) {
                        performSync(scanner);
                    }
                } else if (choice == 2) {
                    // 进行同步
                    performSync(scanner);
                    // 询问是否查看日志
                    if (askToProceed(scanner, "是否查看近期操作记录？(yes/no): ")) {
                        printRecentOperations();
                    }
                } else if (choice == 3) {
                    System.out.println("程序退出。");
                    break;
                } else {
                    System.out.println("无效选择，请重新输入。");
                }
            }
        } catch (IOException e) {
            System.err.println("发生IO异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void readDeployFile() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("Deploy.txt"));
        if (lines.size() < 2) {
            throw new IOException("Deploy.txt文件内容格式错误，至少应包含两行路径。");
        }
        mainDir = lines.get(0).trim();
        syncDir = lines.get(1).trim();
    }

    private static List<String> compareDirectories(File main, File sync) throws IOException {
        List<String> differences = new ArrayList<>();
        compareFiles(main, sync, differences, "");
        return differences;
    }

    private static void compareFiles(File main, File sync, List<String> differences, String path) throws IOException {
        if (main.isDirectory()) {
            if (!sync.exists()) {
                differences.add("仅在主文件夹中存在: " + path + main.getName());
            } else {
                for (File file : Objects.requireNonNull(main.listFiles())) {
                    compareFiles(file, new File(sync, file.getName()), differences, path + main.getName() + "/");
                }
                for (File file : Objects.requireNonNull(sync.listFiles())) {
                    if (!new File(main, file.getName()).exists()) {
                        differences.add("仅在同步文件夹中存在: " + path + file.getName());
                    }
                }
            }
        } else {
            if (!sync.exists() || Files.mismatch(main.toPath(), sync.toPath()) != -1L) {
                differences.add("文件不同: " + path + main.getName());
            }
        }
    }

    private static void printDifferences(List<String> differences) {
        if (differences.isEmpty()) {
            System.out.println("两个文件夹内容一致。");
        } else {
            System.out.println("文件夹内容不同：");
            for (String diff : differences) {
                System.out.println(diff);
            }
        }
    }

    private static void performSync(Scanner scanner) throws IOException {
        List<String> differences = compareDirectories(new File(mainDir), new File(syncDir));
        printDifferences(differences);

        if (askToProceed(scanner, "是否进行同步？(yes/no): ")) {
            List<String> operations = new ArrayList<>();
            backupAndSync(new File(mainDir), new File(syncDir), operations);
            logOperation(operations);
            System.out.println("同步完成！");
        } else {
            System.out.println("同步取消。");
        }
    }

    private static boolean askToProceed(Scanner scanner, String message) {
        System.out.print(message);
        String input = scanner.nextLine();
        return input.equalsIgnoreCase("yes");
    }

    private static void backupAndSync(File main, File sync, List<String> operations) throws IOException {
        String backupDirName = new SimpleDateFormat("yyyyMMdd").format(new Date()) + getRandomString(3);
        File backupDir = new File("PreviousBackups/" + backupDirName);
        backupDir.mkdirs();

        backupAndDelete(sync, backupDir, operations);
        copyFiles(main, sync, operations);
    }

    private static void backupAndDelete(File file, File backup, List<String> operations) throws IOException {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                backupAndDelete(child, new File(backup, child.getName()), operations);
            }
            file.delete();
        } else {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            operations.add("备份并删除: " + file.getPath());
            file.delete();
        }
    }

    private static void copyFiles(File source, File destination, List<String> operations) throws IOException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            for (File file : Objects.requireNonNull(source.listFiles())) {
                copyFiles(file, new File(destination, file.getName()), operations);
            }
        } else {
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            operations.add("复制文件: " + source.getPath() + " 到 " + destination.getPath());
        }
    }

    private static String getRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder randomString = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            randomString.append(characters.charAt(random.nextInt(characters.length())));
        }
        return randomString.toString();
    }

    private static void logOperation(List<String> operations) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String randomString = getRandomString(3);
        StringBuilder logContent = new StringBuilder();
        logContent.append(timestamp).append("-").append(randomString).append("-同步操作：\n");
        for (String operation : operations) {
            logContent.append(operation).append("\n");
        }

        Files.write(Paths.get(LOG_FILE), logContent.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void printRecentOperations() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(LOG_FILE));
        if (lines.isEmpty()) {
            System.out.println("没有操作记录。");
        } else {
            System.out.println("近期操作记录：");
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }
}
