import { Pressable, StyleSheet, Text, View } from "react-native";
import { MaterialIcons } from "@expo/vector-icons";

export function ScreenHeader({ palette, title, onLeftPress, onRightPress, leftIcon = "settings", rightIcon }: { palette: any; title: string; onLeftPress?: () => void; onRightPress?: () => void; leftIcon?: keyof typeof MaterialIcons.glyphMap; rightIcon?: keyof typeof MaterialIcons.glyphMap; }) {
  return (
    <View style={styles.wrap}>
      <Pressable style={styles.icon} onPress={onLeftPress}>
        <MaterialIcons name={leftIcon} size={24} color={palette.primaryContainer} />
      </Pressable>
      <Text style={[styles.title, { color: palette.primary }]}>{title}</Text>
      {rightIcon ? (
        <Pressable style={styles.icon} onPress={onRightPress}>
          <MaterialIcons name={rightIcon} size={24} color={palette.primaryContainer} />
        </Pressable>
      ) : (
        <View style={styles.icon} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 18,
    paddingVertical: 12,
  },
  icon: {
    width: 40,
    height: 40,
    alignItems: "center",
    justifyContent: "center",
  },
  title: {
    fontFamily: "Manrope_800ExtraBold",
    fontSize: 24,
  },
});
