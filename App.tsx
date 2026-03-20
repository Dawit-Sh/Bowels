import { useEffect, useState } from "react";
import { Alert, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import * as DocumentPicker from "expo-document-picker";
import * as FileSystem from "expo-file-system/legacy";
import * as Sharing from "expo-sharing";
import * as SystemUI from "expo-system-ui";
import { useFonts, Manrope_400Regular, Manrope_500Medium, Manrope_700Bold, Manrope_800ExtraBold } from "@expo-google-fonts/manrope";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";

import { AppProvider, useApp, useResolvedThemeMode } from "./store/AppProvider";
import { buildPalette } from "./src/theme";
import { BottomNav } from "./components/BottomNav";
import { ScreenHeader } from "./components/ScreenHeader";
import { HomeScreen } from "./screens/HomeScreen";
import { ActiveSessionScreen } from "./screens/ActiveSessionScreen";
import { QuestionsScreen } from "./screens/QuestionsScreen";
import { HistoryScreen } from "./screens/HistoryScreen";
import { AnalyticsScreen } from "./screens/AnalyticsScreen";
import { WeeklyWrappedScreen } from "./screens/WeeklyWrappedScreen";
import { HealthInfoScreen } from "./screens/HealthInfoScreen";
import { SettingsScreen } from "./screens/SettingsScreen";
import { exportArchiveData, importArchiveData } from "./db/repository";

function AppShell() {
  const app = useApp();
  const themeMode = useResolvedThemeMode(app.settings.themeMode);
  const palette = buildPalette(themeMode, app.settings.accent);
  const [liveSeconds, setLiveSeconds] = useState(0);
  const latestHealth = app.dailyHealth[0]
    ? {
        water: app.dailyHealth[0].water,
        fiber: app.dailyHealth[0].fiber,
        meals: app.dailyHealth[0].meals,
        stress: app.dailyHealth[0].stress,
        sleep: app.dailyHealth[0].sleep,
        exercise: app.dailyHealth[0].exercise,
      }
    : {
        water: "Okay",
        fiber: "Medium",
        meals: "Balanced",
        stress: "Low",
        sleep: "Fair",
        exercise: "Light",
      };

  useEffect(() => {
    SystemUI.setBackgroundColorAsync(palette.background).catch(() => undefined);
  }, [palette.background]);

  useEffect(() => {
    if (!app.activeDraft.startTime || app.screen !== "active") {
      setLiveSeconds(app.activeDraft.durationSeconds);
      return;
    }

    const sync = () => {
      setLiveSeconds(Math.max(0, Math.floor((Date.now() - new Date(app.activeDraft.startTime as string).getTime()) / 1000)));
    };
    sync();
    const intervalId = setInterval(sync, 1000);
    return () => clearInterval(intervalId);
  }, [app.activeDraft.durationSeconds, app.activeDraft.startTime, app.screen]);

  const timerLabel = `${String(Math.floor(liveSeconds / 60)).padStart(2, "0")}:${String(liveSeconds % 60).padStart(2, "0")}`;

  const exportData = async () => {
    try {
      const archive = await exportArchiveData();
      const uri = `${FileSystem.cacheDirectory}bowels-export-${Date.now()}.json`;
      await FileSystem.writeAsStringAsync(uri, JSON.stringify(archive, null, 2));
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(uri);
      } else {
        Alert.alert("Export ready", uri);
      }
    } catch {
      Alert.alert("Export failed", "Unable to create a backup right now.");
    }
  };

  const importData = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({ type: ["application/json", "text/json"], copyToCacheDirectory: true });
      if (result.canceled || !result.assets[0]) {
        return;
      }
      const content = await FileSystem.readAsStringAsync(result.assets[0].uri);
      await importArchiveData(JSON.parse(content));
      await app.refresh();
      app.setScreen("history");
      Alert.alert("Import complete", "Backup data was merged into the local database.");
    } catch {
      Alert.alert("Import failed", "The selected file is not a valid Bowels archive.");
    }
  };

  const offTab = app.screen === "weekly" || app.screen === "health" || app.screen === "settings" || app.screen === "questions";

  if (app.loading) {
    return <SafeAreaView style={{ flex: 1, backgroundColor: palette.background }} edges={["top", "left", "right"]} />;
  }

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: palette.background }} edges={["top", "left", "right"]}>
      <StatusBar style={themeMode === "dark" ? "light" : "dark"} />
      <ScreenHeader
        palette={palette}
        title={app.screen === "history" ? "Data & Privacy" : app.screen === "analytics" ? "Insights" : "Bowels"}
        leftIcon={offTab ? "arrow-back" : "settings"}
        onLeftPress={() => app.setScreen(offTab ? "home" : "settings")}
      />
      <View style={{ flex: 1 }}>
        {app.screen === "home" ? <HomeScreen palette={palette} sessions={app.sessions} insights={app.insights} analytics={app.analytics} dailyHealth={latestHealth} setScreen={app.setScreen} startSession={app.startSession} saveDailyHealth={app.saveDailyHealth} quickLogBowel={app.quickLogBowel} /> : null}
        {app.screen === "active" ? <ActiveSessionScreen palette={palette} timerLabel={timerLabel} draft={app.activeDraft} updateDraft={app.updateDraft} startSession={app.startSession} finishSession={app.finishSession} cancelSession={app.cancelSession} /> : null}
        {app.screen === "questions" ? <QuestionsScreen palette={palette} draft={app.activeDraft} updateDraft={app.updateDraft} saveQuestions={app.saveQuestions} /> : null}
        {app.screen === "history" ? <HistoryScreen palette={palette} sessions={app.sessions} onExport={() => void exportData()} onImport={() => void importData()} /> : null}
        {app.screen === "analytics" ? <AnalyticsScreen palette={palette} analytics={app.analytics} insights={app.insights} /> : null}
        {app.screen === "weekly" ? <WeeklyWrappedScreen palette={palette} analytics={app.analytics} /> : null}
        {app.screen === "health" ? <HealthInfoScreen palette={palette} /> : null}
        {app.screen === "settings" ? <SettingsScreen palette={palette} settings={app.settings} setThemeMode={app.setThemeMode} setAccent={app.setAccent} setReminderHour={app.setReminderHour} /> : null}
      </View>
      <BottomNav palette={palette} screen={app.screen === "questions" ? "active" : app.screen} setScreen={app.setScreen} />
    </SafeAreaView>
  );
}

export default function App() {
  const [fontsLoaded] = useFonts({
    Manrope_400Regular,
    Manrope_500Medium,
    Manrope_700Bold,
    Manrope_800ExtraBold,
  });

  if (!fontsLoaded) {
    return null;
  }

  return (
    <SafeAreaProvider>
      <AppProvider>
        <AppShell />
      </AppProvider>
    </SafeAreaProvider>
  );
}
