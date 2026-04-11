import { Modal, Pressable, ScrollView, StyleSheet, Text, View, Animated } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";
import { useEffect, useRef } from "react";
import { OptionChip } from "./UI";

const healthOptions = {
  sleep: ["Poor", "Fair", "Great"],
  fiber: ["Low", "Medium", "High"],
  water: ["Low", "Okay", "Great"],
  caffeine: ["None", "Low", "Medium", "High"],
  stress: ["Low", "Medium", "High"],
  mood: ["Happy", "Neutral", "Sad", "Anxious", "Stressed"],
} as const;

export function DailyHealthModal({
  visible,
  palette,
  dailyHealth,
  onUpdate,
  onClose,
}: {
  visible: boolean;
  palette: any;
  dailyHealth: any;
  onUpdate: (field: string, value: string) => void;
  onClose: () => void;
}) {
  const scaleAnim = useRef(new Animated.Value(0.9)).current;
  const opacityAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (visible) {
      Animated.parallel([
        Animated.spring(scaleAnim, {
          toValue: 1,
          friction: 10,
          tension: 50,
          useNativeDriver: true,
        }),
        Animated.timing(opacityAnim, {
          toValue: 1,
          duration: 200,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(scaleAnim, {
          toValue: 0.9,
          duration: 150,
          useNativeDriver: true,
        }),
        Animated.timing(opacityAnim, {
          toValue: 0,
          duration: 150,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [visible, scaleAnim, opacityAnim]);

  return (
    <Modal visible={visible} transparent animationType="none" onRequestClose={onClose}>
      <Animated.View style={[styles.overlay, { opacity: opacityAnim }]}>
        <Animated.View style={[styles.modal, { backgroundColor: palette.surface, transform: [{ scale: scaleAnim }] }]}>
          <View style={styles.header}>
            <View style={styles.headerContent}>
              <MaterialIcons name="wb-sunny" size={28} color={palette.primary} />
              <Text style={[styles.title, { color: palette.onSurface }]}>Daily Health Check</Text>
            </View>
            <Pressable onPress={onClose} style={styles.closeButton}>
              <MaterialIcons name="close" size={24} color={palette.onSurfaceVariant} />
            </Pressable>
          </View>

          <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
            <Text style={[styles.subtitle, { color: palette.onSurfaceVariant }]}>
              Take a moment to track your wellness for better insights
            </Text>

            {Object.entries(healthOptions).map(([key, options]) => (
              <View key={key} style={styles.fieldBlock}>
                <Text style={[styles.fieldLabel, { color: palette.onSurface }]}>
                  {key === "sleep" ? "Last Night's Sleep" : 
                   key === "fiber" ? "Today's Fiber Intake" :
                   key === "water" ? "Water Intake" :
                   key === "caffeine" ? "Caffeine Consumption" :
                   key === "stress" ? "Stress Level" :
                   "Mood"}
                </Text>
                <View style={styles.chips}>
                  {options.map((option) => (
                    <OptionChip
                      key={option}
                      palette={palette}
                      label={option}
                      active={dailyHealth[key] === option}
                      onPress={() => onUpdate(key, option)}
                    />
                  ))}
                </View>
              </View>
            ))}

            <Pressable
              style={[styles.saveButton, { backgroundColor: palette.primary }]}
              onPress={onClose}
            >
              <Text style={[styles.saveText, { color: palette.onPrimary }]}>Done</Text>
            </Pressable>
          </ScrollView>
        </Animated.View>
      </Animated.View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.5)",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  modal: {
    width: "100%",
    maxWidth: 500,
    maxHeight: "90%",
    borderRadius: 28,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.3,
    shadowRadius: 16,
    elevation: 10,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    padding: 24,
    paddingBottom: 16,
  },
  headerContent: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  title: {
    fontFamily: "Manrope_800ExtraBold",
    fontSize: 24,
  },
  closeButton: {
    padding: 4,
  },
  content: {
    paddingHorizontal: 24,
    paddingBottom: 24,
  },
  subtitle: {
    fontFamily: "Manrope_400Regular",
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 24,
  },
  fieldBlock: {
    marginBottom: 24,
  },
  fieldLabel: {
    fontFamily: "Manrope_700Bold",
    fontSize: 16,
    marginBottom: 12,
  },
  chips: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  saveButton: {
    paddingVertical: 16,
    borderRadius: 999,
    alignItems: "center",
    marginTop: 8,
    marginBottom: 16,
  },
  saveText: {
    fontFamily: "Manrope_700Bold",
    fontSize: 16,
  },
});
