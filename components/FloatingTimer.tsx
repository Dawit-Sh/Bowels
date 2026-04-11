import { useEffect, useRef } from "react";
import { Animated, PanResponder, Pressable, StyleSheet, Text, InteractionManager } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";

export function FloatingTimer({
  palette,
  timerLabel,
  onFinish,
  onOpen,
}: {
  palette: any;
  timerLabel: string;
  onFinish: () => void;
  onOpen: () => void;
}) {
  const pan = useRef(new Animated.ValueXY({ x: 0, y: 100 })).current;
  const pulseAnim = useRef(new Animated.Value(1)).current;
  const scaleAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    // Entry animation
    InteractionManager.runAfterInteractions(() => {
      Animated.spring(scaleAnim, {
        toValue: 1,
        friction: 8,
        tension: 40,
        useNativeDriver: true,
      }).start();
    });

    // Pulse animation
    const pulseAnimation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 1.05,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    );
    
    pulseAnimation.start();
    
    return () => {
      pulseAnimation.stop();
    };
  }, [pulseAnim, scaleAnim]);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: () => {
        pan.setOffset({
          x: (pan.x as any)._value,
          y: (pan.y as any)._value,
        });
      },
      onPanResponderMove: Animated.event([null, { dx: pan.x, dy: pan.y }], {
        useNativeDriver: false,
      }),
      onPanResponderRelease: () => {
        pan.flattenOffset();
      },
    })
  ).current;

  return (
    <Animated.View
      style={[
        styles.container,
        {
          transform: [
            { translateX: pan.x },
            { translateY: pan.y },
            { scale: Animated.multiply(pulseAnim, scaleAnim) },
          ],
        },
      ]}
      {...panResponder.panHandlers}
    >
      <Pressable
        style={[styles.bubble, { backgroundColor: palette.primary }]}
        onPress={onOpen}
      >
        <MaterialIcons name="timer" size={20} color={palette.onPrimary} />
        <Text style={[styles.timerText, { color: palette.onPrimary }]}>
          {timerLabel}
        </Text>
      </Pressable>
      <Pressable
        style={[styles.finishButton, { backgroundColor: palette.secondary }]}
        onPress={onFinish}
      >
        <MaterialIcons name="check" size={16} color={palette.onPrimary} />
      </Pressable>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: "absolute",
    right: 16,
    top: 100,
    zIndex: 9999,
    elevation: 10,
  },
  bubble: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 999,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  timerText: {
    fontFamily: "Manrope_700Bold",
    fontSize: 16,
  },
  finishButton: {
    position: "absolute",
    top: -8,
    right: -8,
    width: 32,
    height: 32,
    borderRadius: 999,
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
});
