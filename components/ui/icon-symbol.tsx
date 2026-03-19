import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { ComponentProps } from 'react';
import { OpaqueColorValue, type StyleProp, type TextStyle } from 'react-native';

export type IconSymbolName =
  | 'house.fill'
  | 'clock.fill'
  | 'chart.bar.fill'
  | 'sparkles'
  | 'gearshape.fill'
  | 'person.circle'
  | 'bell'
  | 'chevron.left'
  | 'chevron.right'
  | 'star.fill'
  | 'bolt.fill'
  | 'drop.circle.fill'
  | 'leaf.fill'
  | 'star.circle.fill'
  | 'crown.fill'
  | 'figure.walk.circle.fill'
  | 'sun.max.fill'
  | 'drop.fill'
  | 'brain.head.profile'
  | 'scale.3d'
  | 'flame.fill'
  | 'line.3.horizontal';

type IconMapping = Record<IconSymbolName, ComponentProps<typeof MaterialIcons>['name']>;

/**
 * Add your app icons to mappings here.
 * See Material Icons in https://icons.expo.fyi (MaterialIcons).
 */
const MAPPING = {
  'house.fill': 'home',
  'clock.fill': 'schedule',
  'chart.bar.fill': 'bar-chart',
  sparkles: 'auto-awesome',
  'gearshape.fill': 'settings',
  'person.circle': 'person',
  bell: 'notifications-none',
  'chevron.left': 'chevron-left',
  'chevron.right': 'chevron-right',
  'star.fill': 'star',
  'bolt.fill': 'bolt',
  'drop.circle.fill': 'water-drop',
  'leaf.fill': 'eco',
  'star.circle.fill': 'stars',
  'crown.fill': 'emoji-events',
  'figure.walk.circle.fill': 'directions-walk',
  'sun.max.fill': 'wb-sunny',
  'drop.fill': 'water-drop',
  'brain.head.profile': 'psychology',
  'scale.3d': 'balance',
  'flame.fill': 'local-fire-department',
  'line.3.horizontal': 'menu',
} as IconMapping;

/**
 * An icon component that uses native SF Symbols on iOS, and Material Icons on Android and web.
 * This ensures a consistent look across platforms, and optimal resource usage.
 * Icon `name`s are based on SF Symbols and require manual mapping to Material Icons.
 */
export function IconSymbol({
  name,
  size = 24,
  color,
  style,
}: {
  name: IconSymbolName;
  size?: number;
  color: string | OpaqueColorValue;
  style?: StyleProp<TextStyle>;
}) {
  return <MaterialIcons color={color} size={size} name={MAPPING[name]} style={style} />;
}
