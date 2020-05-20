package pl.fanta.steamcmd;

import pl.fanta.utils.ConditionalSleep;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WorkshopTask {

    //    private static final File STEAM_CMD_PATH = new File(System.getProperty("user.dir") + "\\src\\main\\resources\\steamcmd");
    private static final String STEAMCMD_DIR = "steamcmd\\";
    private static final String BASIC_PARAMS = STEAMCMD_DIR + "steamcmd.exe +login anonymous +set_download_throttle 2 false +workshop_download_item 232090 ";

    public final CompletableFuture<List<File>> checkMapName(String itemId) {
        try {
            System.out.println("Starting new thread");
            // 1. Execute bash command
            Process process = Runtime.getRuntime().exec(BASIC_PARAMS + itemId);
            // 2. Sleep till folder for that item has been created (meaning file name has been set)
            ConditionalSleep.sleepUntil(() -> new File(STEAMCMD_DIR + "steamapps\\workshop\\downloads\\232090\\" + itemId).exists(), 20_000, 200);
            // 3. We dont need the process anymore so destroy it -> it should kill also the steamcmd bash app
            process.destroyForcibly();

            // 4. Get result -> we dont need it anyway since it executes external bash program
            // StreamGobbler streamGobbler =
            // new StreamGobbler(process.getInputStream(), System.out::println);
            // Executors.newSingleThreadExecutor().submit(streamGobbler);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 5. Navigate to the path and penetrate to read filename(s)
        final Path mapName = Paths.get(STEAMCMD_DIR + "steamapps\\workshop\\downloads\\232090\\" + itemId);

        // 6. Create array list that we gonna return at the end -> array list beacuse some workshop items contain multiple maps
        final List<File> fileNames = new ArrayList<>();

        // 7. Recursive open folders in specified directory till we reach the endm then map it to file, filter only map files and add to the list
        try {
            Files.walk(mapName)
                    .map(Path::toFile)
                    .filter(file -> file.getName().endsWith(".kfm"))
                    .forEach(file -> {
                        if (file.exists()) {
                            fileNames.add(file);
                            file.delete();
                        } else {
                            System.out.println("!!Cant find a file!!");
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 8. End task and return list
        return CompletableFuture.completedFuture(fileNames);
    }

    public final void updateSteamCMD() {
        // 1. Start steamCMD and attach InputStreamer
        try {
            final Process steamCmd = Runtime.getRuntime().exec(STEAMCMD_DIR + "steamcmd.exe");

            final BufferedReader br = new BufferedReader(new InputStreamReader(steamCmd.getInputStream()));

            br.lines().forEach(s -> {

                if (s.contains("Loading Steam API")) {

                    System.out.println("Updated SteamCMD");

                    try {
                        // there is still some processing going so give it ~5 secs to finish
                        Thread.sleep(5000);

                        // destroy steam bootstraper
                        steamCmd.destroyForcibly();

                        // destroy steamcmd automatically ran after update
                        Runtime.getRuntime().exec("taskkill /f /im steamcmd.exe");

                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }

                }

            });

            // close reader
            br.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public final boolean isFirstLaunch() {
        final File dir = new File(STEAMCMD_DIR);
        final File[] files = dir.listFiles();

        if (!dir.exists() || files == null) {
            System.out.println("You are missing steamcmd directory, redownload the program and follow instructions");
            System.exit(0);
        }


        return files.length <= 1;
    }


}
