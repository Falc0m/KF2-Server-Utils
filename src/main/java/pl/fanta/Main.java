package pl.fanta;


import pl.fanta.steamcmd.WorkshopTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final WorkshopTask workshopTask = new WorkshopTask();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(7);

    public static void main(String[] args) {
        final List<String> itemIds = new ArrayList<>();
        final List<File> mapNames = new ArrayList<>();
        final List<CompletableFuture<List<File>>> futures = new ArrayList<>();

        // 0. Find out if SteamCMD needs and update
        if (workshopTask.isFirstLaunch()) {
            System.out.println("Updating steamCMD");
            workshopTask.updateSteamCMD();
        }

        // 1. Read file and add workshopItem ID's to the list
        try (FileReader fr = new FileReader("workshop.txt");
             BufferedReader br = new BufferedReader(fr)) {

            String next;

            while ((next = br.readLine()) != null) {
                itemIds.add(next.trim());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. Multithreaded getting map names
        for (final String s : itemIds) {
            // this is where multithreading should step in
            try {
                // create list to get map names
//                System.out.println("Trying to start async thread on item=" + s);
//                final List<File> taskNames = CompletableFuture.supplyAsync(() -> checkMap(s), threadPool).get();
                final CompletableFuture<List<File>> asd = CompletableFuture.supplyAsync(() -> checkMap(s), threadPool);
                futures.add(asd);

                //merge lists
//                System.out.println("Trying to merge lists on item=" + s);
//                mapNames.addAll(list);

//                mapNames.addAll(taskNames);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }


        // 3. We added all tasks to the threadPool, now we have to wait till all task are finished
        // multiple ways to achieve it
        //.join
        //.awaitTermination
        awaitTerminationAfterShutdown(threadPool);

        // merge lists
        futures.forEach(list -> {
            try {
                mapNames.addAll(list.get());
            } catch (Exception e) {
                e.printStackTrace();
            }

        });


        // 4. Sorting all maps alphabetically
        mapNames.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));


        // 5. Start writing to the file
        try (FileWriter fw = new FileWriter(new File("output.txt"));
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {

            // declare stringbuilder we going to use for map cycle
            final StringBuilder sb = new StringBuilder();

            // printing map cycle
            for (File file : mapNames) {
                sb.append("\"").append(file.getName().replaceAll(".kfm", "")).append("\",");
            }

            // remove last "," at the very end of the list
            final String sanitizeCustomMaps = sb.toString();

            // GameMapCycles=(Maps=("---- OFFICIAL ----","KF-Airship","KF-Bioticslab","KF-BurningParis","KF-Catacombs","KF-ContainmentStation","KF-EvacuationPoint","KF-Farmhouse","KF-HostileGrounds","KF-InfernalRealm","KF-KrampusLair","KF-Lockdown","KF-MonsterBall","KF-Nightmare","KF-Nuked","KF-Outpost","KF-Prison","KF-ShoppingSpree","KF-Spillway","KF-SteamFortress","KF-TheDescent","KF-TragicKingdom","KF-VolterManor","KF-ZedLanding","---- CUSTOM ----","KF-Abyss","KF-ApexB5-FIX","KF-Arendelle","KF-BeforeDawn","KF-BioticsLab2009-F1","KF-Border_Wall","KF-Cross","KF-De_Dust","KF-De_Dust2-CDEdit","KF-De_Nuke-CDEdit","KF-DeepingWall_zfix","KF-Desolation","KF-Dust2Classic","KF-GridLock","KF-inferno","KF-IronCross","KF-KF1-FrightYard","KF-Neon_Holdout","KF-QuarantineBreach","KF-Random_Holdout","KF-RatsCafeXmas","KF-RoundAbout","KF-Slaughterhouse-v2-2","KF-The-Killing-Temple","KF-ThrillsChills","KF-Toxic_Cavern","KF-TragicKingdom","KF-Treatment_Station","KF-Ville_de_Doom")
            pw.println("GameMapCycles=(Maps=(\"---- OFFICIAL ----\",\"KF-Airship\",\"KF-AshwoodAsylum\",\"KF-Biolapse\",\"KF-Bioticslab\",\"KF-BlackForest\",\"KF-BurningParis\",\"KF-Catacombs\",\"KF-ContainmentStation\",\"KF-DieSector\",\"KF-EvacuationPoint\",\"KF-Farmhouse\",\"KF-HostileGrounds\",\"KF-InfernalRealm\",\"KF-KrampusLair\",\"KF-Lockdown\",\"KF-MonsterBall\",\"KF-Nightmare\",\"KF-Nuked\",\"KF-Outpost\",\"KF-PowerCore_Holdout\",\"KF-Prison\",\"KF-Sanitarium\",\"KF-Santasworkshop\",\"KF-ShoppingSpree\", \"KF-Spillway\", \"KF-SteamFortress\", \"KF-TheDescent\",\"KF-TragicKingdom\",\"KF-VolterManor\",\"KF-ZedLanding\",\"---- CUSTOM ----\","
                    + sanitizeCustomMaps.substring(0, sanitizeCustomMaps.length() - 1)
                    + "))\r\n");


            // printing each map
            // [KF-Abyss KFMapSummary]
            // MapName=KF-Abyss
            // MapAssociation=0
            // ScreenshotPathName=UI_MapPreview_TEX.UI_MapPreview_Placeholder
            for (File file : mapNames) {
                final String sanitizedMapName = file.getName().replaceAll(".kfm", "");
                pw.println("[" + sanitizedMapName + " KFMapSummary]");
                pw.println("MapName=" + sanitizedMapName);
                pw.println("MapAssociation=0");
                pw.println("ScreenshotPathName=UI_MapPreview_TEX.UI_MapPreview_Placeholder\r\n");
            }

            // print interface needed for workshop downloads
            pw.println("[OnlineSubsystemSteamworks.KFWorkshopSteamworks]");

            // print every workshop item in KFEngine.ini
            // ServerSubscribedWorkshopItems=123456
            for (String s : itemIds) {
                pw.println("ServerSubscribedWorkshopItems=" + s.trim());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 6. Cleanup
        final List<Path> trash = Arrays.asList(
                Paths.get("steamcmd\\userdata"),
                Paths.get("steamcmd\\steamapps"),
                Paths.get("steamcmd\\depotcache"),
                Paths.get("steamcmd\\dumps")
        );

        // delete trash files
        trash.forEach(Main::deleteDirectoryStream);

        // terminate all steamcmd.exe if havent
        try {
            Runtime.getRuntime().exec("taskkill /f /im steamcmd.exe");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static void awaitTerminationAfterShutdown(ExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.MINUTES)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static List<File> checkMap(String s) {
        try {
            return workshopTask.checkMapName(s).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private static void deleteDirectoryStream(Path path) {
        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
