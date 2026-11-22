import { fail } from 'k6';
import {
  finishObservations,
  observationFlow,
  prepareObservations,
  rabbitMonitorFlow,
  safeSystemTags,
} from './common.js';

const rate = Number(__ENV.OBSERVATION_RATE || 10);
const duration = __ENV.DURATION || '15m';
const processingTimeoutSeconds = Number(__ENV.PROCESSING_CONFIRM_TIMEOUT_SEC || 0);
const pollIntervalSeconds = Number(__ENV.STATUS_POLL_INTERVAL_SEC || 2);
const observationGrace = `${processingTimeoutSeconds + pollIntervalSeconds}s`;
const monitorDuration = `${duration}${processingTimeoutSeconds}s`;
const imagePath = __ENV.TEST_IMAGE_PATH;
if (!imagePath) {
  throw new Error('TEST_IMAGE_PATH is required');
}
const image = open(imagePath, 'b');

export const options = {
  systemTags: safeSystemTags,
  scenarios: {
    observations: {
      executor: 'constant-arrival-rate',
      rate,
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
};

export function setup() {
  return prepareObservations();
}

export default function (data) {
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
