import { Transition, Variants } from "framer-motion";

/**
 * Enterprise Motion Design Tokens
 * Strict cubic-bezier(0.4, 0, 0.2, 1) easing and standardized duration tiers.
 */
export const MOTION_EASING: [number, number, number, number] = [0.4, 0, 0.2, 1];

export const MOTION_DURATION = {
  micro: 0.15, // 150ms: hover, button press, icon toggle
  macro: 0.3,  // 300ms: page transitions, modal/drawer open
  threat: 0.4, // 400ms: threat alert feedback with spring feel
  stagger: 0.05, // 50ms: stagger interval between list/grid cards
};

export const defaultTransition: Transition = {
  duration: MOTION_DURATION.macro,
  ease: MOTION_EASING,
};

export const microTransition: Transition = {
  duration: MOTION_DURATION.micro,
  ease: MOTION_EASING,
};

export const springThreatTransition: Transition = {
  type: "spring",
  stiffness: 380,
  damping: 24,
  duration: MOTION_DURATION.threat,
};

/**
 * Page Transitions (Overview -> Inspector -> Process Monitor)
 * Gracefully degrades to simple fade if shouldReduceMotion is true.
 */
export const getPageVariants = (reduceMotion: boolean): Variants => ({
  initial: {
    opacity: 0,
    y: reduceMotion ? 0 : 10,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: MOTION_DURATION.macro,
      ease: MOTION_EASING,
    },
  },
  exit: {
    opacity: 0,
    y: reduceMotion ? 0 : -8,
    transition: {
      duration: MOTION_DURATION.micro,
      ease: MOTION_EASING,
    },
  },
});

/**
 * Stagger Containers & Cards
 */
export const staggerContainer: Variants = {
  initial: {},
  animate: {
    transition: {
      staggerChildren: MOTION_DURATION.stagger,
    },
  },
};

export const getStaggerItem = (reduceMotion: boolean): Variants => ({
  initial: {
    opacity: 0,
    y: reduceMotion ? 0 : 20,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: MOTION_DURATION.macro,
      ease: MOTION_EASING,
    },
  },
});

/**
 * List Row Insertion (Slide down from top)
 */
export const getRowVariants = (reduceMotion: boolean): Variants => ({
  initial: {
    opacity: 0,
    y: reduceMotion ? 0 : -12,
  },
  animate: {
    opacity: 1,
    y: 0,
    transition: {
      duration: MOTION_DURATION.macro,
      ease: MOTION_EASING,
    },
  },
  exit: {
    opacity: 0,
    scale: reduceMotion ? 1 : 0.98,
    transition: {
      duration: MOTION_DURATION.micro,
      ease: MOTION_EASING,
    },
  },
});

/**
 * Threat Shake & Pop Variants
 */
export const threatShakeVariants: Variants = {
  initial: { x: 0 },
  shake: {
    x: [0, -6, 6, -3, 3, 0],
    transition: {
      duration: 0.35,
      ease: MOTION_EASING,
    },
  },
};

export const threatBadgePulse: Variants = {
  animate: {
    opacity: [0.8, 1, 0.8],
    transition: {
      duration: 2.2,
      repeat: Infinity,
      ease: "easeInOut",
    },
  },
};
