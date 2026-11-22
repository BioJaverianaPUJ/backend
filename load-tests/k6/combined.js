import { fail } from 'k6';
import {
  adminFlow,
  finishObservations,
  mobileFlow,
  observationFlow,
  prepareCombined,
  prepareObservations,
  rabbitMonitorFlow,
  safeSystemTags,
} from './common.js';

const duration = __ENV.DURATION || '15m';
const mobileVus = Number(__ENV.MOBILE_VUS || 50);
const adminVus = Number(__ENV.ADMIN_VUS || 3);
const observationRate = Number(__ENV.OBSERVATION_RATE || 10);
const processingTimeoutSeconds = Number(__ENV.PROCESSING_CONFIRM_TIMEOUT_SEC || 0);
const pollIntervalSeconds = Number(__ENV.STATUS_POLL_INTERVAL_SEC || 2);
const observationGrace = `${processingTimeoutSeconds + pollIntervalSeconds}s`;
const monitorDuration = `${duration}${processingTimeoutSeconds}s`;
const imagePath = __ENV.TEST_IMAGE_PATH;
if (!imagePath) {
  throw new Error('TEST_IMAGE_PATH is required for the combined run');
}
const image = open(imagePath, 'b');

export const options = {
  systemTags: safeSystemTags,
  scenarios: {
    mobile: {
      executor: 'constant-vus',
      exec: 'mobile',
      vus: mobileVus,
      duration,
    },
    admin: {
      executor: 'constant-vus',
      exec: 'admin',
      vus: adminVus,
      duration,
    },
    observations: {
      executor: 'constant-arrival-rate',
      exec: 'observations',
      rate: observationRate,
      timeUnit: '1m',
      duration,
      gracefulStop: observationGrace,
      preAllocatedVUs: 10,
      maxVUs: 50,
    },
    rabbit_monitor: {
      executor: 'constant-vus',
      exec: 'monitorRabbit',
      vus: 1,
      duration: monitorDuration,
    },
  },
  thresholds: {
    'http_req_duration{scenario:mobile}': ['p(95)<=1000'],
    'http_req_duration{scenario:admin}': ['p(95)<=2000'],
  },
};

export function setup() {
  const auth = prepareCombined();
  const observation = prepareObservations();
  return {
    ...auth,
    processingTimeoutSeconds: observation.processingTimeoutSeconds,
    rabbitBefore: observation.rabbitBefore,
  };
}

export function mobile(data) {
  mobileFlow(data);
}

export function admin(data) {
  adminFlow(data);
}

export function observations(data) {
  if (!Number.isFinite(Number(__ENV.TEST_LATITUDE)) || !Number.isFinite(Number(__ENV.TEST_LONGITUDE))) {
    fail('TEST_LATITUDE and TEST_LONGITUDE must be valid numbers');
  }
  observationFlow(data, image);
}

export function monitorRabbit() {
  rabbitMonitorFlow();
}

export function teardown(data) {
  finishObservations(data);
}
