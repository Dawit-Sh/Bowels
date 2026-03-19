import React, { useState } from 'react';
import { StyleSheet, Text, LayoutChangeEvent } from 'react-native';
import { GestureDetector, Gesture } from 'react-native-gesture-handler';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withSpring,
  runOnJS,
  interpolateColor,
} from 'react-native-reanimated';
import { IconSymbol } from './icon-symbol';

type Props = {
  isActive: boolean;
  onSwipeSuccess: () => void;
};

const KNOB_SIZE = 52;
const PADDING = 4;

export function SwipeSlider({ isActive, onSwipeSuccess }: Props) {
  const [width, setWidth] = useState(0);
  const maxTranslate = Math.max(0, width - KNOB_SIZE - PADDING * 2);
  const translateX = useSharedValue(isActive ? 1000 : 0);
  const startX = useSharedValue(0);

  React.useEffect(() => {
    if (width > 0) {
      translateX.value = isActive ? maxTranslate : 0;
    }
  }, [isActive, width, maxTranslate, translateX]);

  const handleLayout = (e: LayoutChangeEvent) => {
    setWidth(e.nativeEvent.layout.width);
  };

  const pan = Gesture.Pan()
    .onStart(() => {
      startX.value = translateX.value;
    })
    .onUpdate((event) => {
      let nextX = startX.value + event.translationX;
      nextX = Math.max(0, Math.min(nextX, maxTranslate));
      translateX.value = nextX;
    })
    .onEnd(() => {
      if (!isActive) {
        if (translateX.value > maxTranslate * 0.8) {
          translateX.value = withSpring(maxTranslate);
          runOnJS(onSwipeSuccess)();
        } else {
          translateX.value = withSpring(0);
        }
      } else {
        if (translateX.value < maxTranslate * 0.2) {
          translateX.value = withSpring(0);
          runOnJS(onSwipeSuccess)();
        } else {
          translateX.value = withSpring(maxTranslate);
        }
      }
    });

  const animatedKnobStyle = useAnimatedStyle(() => {
    return { transform: [{ translateX: translateX.value }] };
  });

  const containerStyle = useAnimatedStyle(() => {
    const bg = interpolateColor(
      translateX.value,
      [0, Math.max(1, maxTranslate)],
      isActive ? ['#FF3B30', '#1C1C1E'] : ['#1C1C1E', '#B8FF2D']
    );
    return { backgroundColor: bg };
  });

  return (
    <Animated.View style={[styles.container, containerStyle]} onLayout={handleLayout}>
      <Text style={styles.text}>{isActive ? 'Slide left to finish' : 'Slide right to start'}</Text>
      
      {width > 0 && (
        <GestureDetector gesture={pan}>
          <Animated.View
            style={[
              styles.knob,
              { backgroundColor: isActive ? '#FF3B30' : '#B8FF2D' },
              animatedKnobStyle,
            ]}>
            <IconSymbol name={isActive ? 'chevron.left' : 'chevron.right'} size={24} color="#000" />
          </Animated.View>
        </GestureDetector>
      )}
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    padding: PADDING,
    overflow: 'hidden',
  },
  text: {
    position: 'absolute',
    width: '100%',
    textAlign: 'center',
    color: '#fff',
    fontWeight: '800',
    fontSize: 14,
    zIndex: 0,
    letterSpacing: 0.5,
  },
  knob: {
    width: KNOB_SIZE,
    height: KNOB_SIZE,
    borderRadius: KNOB_SIZE / 2,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 10,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 4,
  },
});
