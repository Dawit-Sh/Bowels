import { Alert, ScrollView, StyleSheet, Text, View } from "react-native";
import { useState } from "react";

import { Card, GradientButton, OptionChip, SectionTitle } from "../components/UI";
import { stoolTypeMeta } from "../src/sessionMeta";
import type { AdaptiveTag, SessionDraft, StoolType } from "../src/types";

const adaptiveTags: AdaptiveTag[] = ["bloating", "straining", "gas"];

export function QuestionsScreen({
  palette,
  draft,
  updateDraft,
  saveQuestions,
  saveDailyHealth,
  dailyHealth,
}: {
  palette: any;
  draft: SessionDraft;
  updateDraft: (patch: Partial<SessionDraft>) => void;
  saveQuestions: () => Promise<void>;
  saveDailyHealth: (input: any) => Promise<void>;
  dailyHealth: any;
}) {
  const [localHealth, setLocalHealth] = useState(dailyHealth);
  const today = new Date().toISOString().slice(0, 10);
  const hasFilledToday = dailyHealth.lastUpdated === today;

  const handleSave = async () => {
    if (!hasFilledToday) {
      Alert.alert(
        "Daily Health Check",
        "Have you updated your daily health check (sleep, fiber, water)?",
        [
          {
            text: "Skip for Now",
            style: "cancel",
            onPress: () => {
              void saveQuestions();
            },
          },
          {
            text: "Yes, Save All",
            onPress: async () => {
              await saveDailyHealth(localHealth);
              await saveQuestions();
            },
          },
        ]
      );
    } else {
      await saveQuestions();
    }
  };

  const updateLocalHealth = (field: string, value: string) => {
    setLocalHealth({ ...localHealth, [field]: value });
  };

  return (
    <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
      <SectionTitle palette={palette} title="Session complete" subtitle={`Duration ${Math.floor(draft.durationSeconds / 60)}:${String(draft.durationSeconds % 60).padStart(2, "0")}`} />
      <Card palette={palette}>
        <Text style={[styles.label, { color: palette.onSurface }]}>Type</Text>
        <View style={styles.chips}>
          {(["urine", "bowel"] as const).map((kind) => (
            <OptionChip
              key={kind}
              palette={palette}
              label={kind}
              active={draft.kind === kind}
              onPress={() => updateDraft({
                kind,
                stoolType: kind === "bowel" ? draft.stoolType ?? 4 : null,
                pain: kind === "bowel" ? draft.pain ?? "none" : null,
                blood: kind === "bowel" ? draft.blood ?? false : null,
                urgency: kind === "bowel" ? draft.urgency ?? 3 : null,
                completion: kind === "bowel" ? draft.completion ?? "normal" : null,
                urineUrgency: kind === "urine" ? draft.urineUrgency ?? "medium" : null,
                urineColor: kind === "urine" ? draft.urineColor ?? "normal" : null,
              })}
            />
          ))}
        </View>
      </Card>

      {draft.kind === "urine" ? (
        <Card palette={palette}>
          <Text style={[styles.label, { color: palette.onSurface }]}>Urgency</Text>
          <View style={styles.chips}>{(["low", "medium", "high"] as const).map((value) => <OptionChip key={value} palette={palette} label={value} active={draft.urineUrgency === value} onPress={() => updateDraft({ urineUrgency: value })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Color</Text>
          <View style={styles.chips}>{(["light", "normal", "dark"] as const).map((value) => <OptionChip key={value} palette={palette} label={value} active={draft.urineColor === value} onPress={() => updateDraft({ urineColor: value })} />)}</View>
        </Card>
      ) : (
        <Card palette={palette}>
          <Text style={[styles.label, { color: palette.onSurface }]}>Stool Type</Text>
          <View style={styles.chips}>{([1, 2, 3, 4, 5, 6, 7] as StoolType[]).map((value) => <OptionChip key={value} palette={palette} label={`${stoolTypeMeta[value].emoji} ${stoolTypeMeta[value].short}`} active={draft.stoolType === value} onPress={() => updateDraft({ stoolType: value })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Pain</Text>
          <View style={styles.chips}>{(["none", "mild", "moderate", "severe"] as const).map((value) => <OptionChip key={value} palette={palette} label={value} active={draft.pain === value} onPress={() => updateDraft({ pain: value })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Blood</Text>
          <View style={styles.chips}>{["No", "Yes"].map((value) => <OptionChip key={value} palette={palette} label={value} active={draft.blood === (value === "Yes")} onPress={() => updateDraft({ blood: value === "Yes" })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Urgency</Text>
          <View style={styles.chips}>{([1, 2, 3, 4, 5] as const).map((value) => <OptionChip key={value} palette={palette} label={String(value)} active={draft.urgency === value} onPress={() => updateDraft({ urgency: value })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Completion</Text>
          <View style={styles.chips}>{(["incomplete", "normal", "urgent"] as const).map((value) => <OptionChip key={value} palette={palette} label={value} active={draft.completion === value} onPress={() => updateDraft({ completion: value })} />)}</View>
          <Text style={[styles.label, { color: palette.onSurface }]}>Adaptive tags</Text>
          <View style={styles.chips}>
            {adaptiveTags.map((tag) => {
              const active = draft.adaptiveTags.includes(tag);
              return (
                <OptionChip
                  key={tag}
                  palette={palette}
                  label={tag}
                  active={active}
                  onPress={() => {
                    const next = active ? draft.adaptiveTags.filter((item) => item !== tag) : [...draft.adaptiveTags, tag].slice(0, 5);
                    if (!active && draft.adaptiveTags.length >= 5) {
                      Alert.alert("Maximum reached", "You can select up to 5 adaptive tags.");
                      return;
                    }
                    updateDraft({ adaptiveTags: next });
                  }}
                />
              );
            })}
          </View>
        </Card>
      )}

      <Card palette={palette}>
        <Text style={[styles.sectionTitle, { color: palette.primary }]}>Quick Health Check</Text>
        <Text style={[styles.subtitle, { color: palette.onSurfaceVariant }]}>
          {hasFilledToday ? "Already filled today ✓" : "Help us provide better insights"}
        </Text>
        
        <Text style={[styles.label, { color: palette.onSurface }]}>Last Night's Sleep</Text>
        <View style={styles.chips}>
          {(["Poor", "Fair", "Great"] as const).map((value) => (
            <OptionChip
              key={value}
              palette={palette}
              label={value}
              active={localHealth.sleep === value}
              onPress={() => updateLocalHealth("sleep", value)}
            />
          ))}
        </View>

        <Text style={[styles.label, { color: palette.onSurface }]}>Today's Fiber Intake</Text>
        <View style={styles.chips}>
          {(["Low", "Medium", "High"] as const).map((value) => (
            <OptionChip
              key={value}
              palette={palette}
              label={value}
              active={localHealth.fiber === value}
              onPress={() => updateLocalHealth("fiber", value)}
            />
          ))}
        </View>

        <Text style={[styles.label, { color: palette.onSurface }]}>Water Intake Today</Text>
        <View style={styles.chips}>
          {(["Low", "Okay", "Great"] as const).map((value) => (
            <OptionChip
              key={value}
              palette={palette}
              label={value}
              active={localHealth.water === value}
              onPress={() => updateLocalHealth("water", value)}
            />
          ))}
        </View>
      </Card>

      <GradientButton palette={palette} label="Save to Journal" icon="save" onPress={() => void handleSave()} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 24, paddingBottom: 160, gap: 20 },
  label: { fontFamily: "Manrope_700Bold", fontSize: 14, marginTop: 12 },
  sectionTitle: { fontFamily: "Manrope_700Bold", fontSize: 18 },
  subtitle: { fontFamily: "Manrope_400Regular", fontSize: 13, marginBottom: 8 },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
});
