import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";

import { Card, GradientButton } from "../components/UI";
import { stoolTypeMeta } from "../src/sessionMeta";
import type { SessionDraft, StoolType } from "../src/types";

export function ActiveSessionScreen({
  palette,
  timerLabel,
  draft,
  updateDraft,
  startSession,
  finishSession,
  cancelSession,
}: {
  palette: any;
  timerLabel: string;
  draft: SessionDraft;
  updateDraft: (patch: Partial<SessionDraft>) => void;
  startSession: () => void;
  finishSession: () => void;
  cancelSession: () => void;
}) {
  const sessionActive = Boolean(draft.startTime);
  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <View style={styles.timerWrap}>
        <Text style={[styles.overline, { color: palette.onSurfaceVariant }]}>Active Session</Text>
        <Text style={[styles.timer, { color: palette.primary }]}>{timerLabel}</Text>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>{sessionActive ? "Finding your rhythm..." : "Ready when you are."}</Text>
      </View>

      <View style={styles.titleBlock}>
        <Text style={[styles.heading, { color: palette.onSurface }]}>How does it feel</Text>
        <Text style={[styles.heading, { color: palette.tertiary }]}>right now?</Text>
      </View>

      <View style={styles.scaleWrap}>
        {([1, 2, 3, 4, 5, 6, 7] as StoolType[]).map((type) => {
          const active = draft.stoolType === type;
          const meta = stoolTypeMeta[type];
          return (
            <Pressable key={type} style={[styles.scaleCard, { backgroundColor: active ? palette.surfaceContainerLowest : palette.surfaceContainerHigh, borderColor: active ? `${palette.primary}55` : "transparent" }]} onPress={() => updateDraft({ kind: "bowel", stoolType: type })}>
              <Text style={styles.emoji}>{meta.emoji}</Text>
              <Text style={[styles.scaleTitle, { color: active ? palette.primary : palette.onSurface }]}>{meta.short}</Text>
              <Text style={[styles.scaleBody, { color: palette.onSurfaceVariant }]}>{meta.description}</Text>
            </Pressable>
          );
        })}
      </View>

      <Card palette={palette}>
        <Text style={[styles.body, { color: palette.onSurfaceVariant }]}>This uses plain-language Bristol descriptions so users do not need to memorize numbered stool types.</Text>
      </Card>

      {sessionActive ? <GradientButton palette={palette} label="Finish Session" icon="check-circle" onPress={finishSession} /> : <GradientButton palette={palette} label="Start Session" icon="play-arrow" onPress={startSession} />}
      {sessionActive ? <Pressable onPress={cancelSession}><Text style={[styles.cancel, { color: palette.onSurfaceVariant }]}>Cancel and discard</Text></Pressable> : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 24 },
  timerWrap: { alignItems: "center", gap: 8, paddingTop: 12 },
  overline: { fontFamily: "Manrope_700Bold", fontSize: 11, textTransform: "uppercase", letterSpacing: 2 },
  timer: { fontFamily: "Manrope_800ExtraBold", fontSize: 68, letterSpacing: -2 },
  body: { fontFamily: "Manrope_400Regular", fontSize: 14, lineHeight: 20 },
  titleBlock: { marginHorizontal: 22 },
  heading: { fontFamily: "Manrope_800ExtraBold", fontSize: 28, lineHeight: 32 },
  scaleWrap: { flexDirection: "row", flexWrap: "wrap", gap: 10 },
  scaleCard: { width: "48%", borderRadius: 24, padding: 16, borderWidth: 2, gap: 6 },
  emoji: { fontSize: 28 },
  scaleTitle: { fontFamily: "Manrope_700Bold", fontSize: 16 },
  scaleBody: { fontFamily: "Manrope_400Regular", fontSize: 12, lineHeight: 17 },
  cancel: { textAlign: "center", fontFamily: "Manrope_700Bold", fontSize: 14 },
});
