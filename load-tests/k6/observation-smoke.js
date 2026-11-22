import { fail } from 'k6';
import {
  finishObservations,
  observationFlow,
  prepareObservations,
  safeSystemTags,
} from './common.js';

const imagePath = __ENV.TEST_IMAGE_PATH;
if (!imagePath) {
  throw new Error('TEST_IMAGE_PATH is required');
}
const image = open(imagePath, 'b');

export const options = {
  systemTags: safeSystemTags,
  scenarios: {
    observation_smoke: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
    },
  },
  thresholds: {
    observation_processing_completed: ['rate>0.99'],
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

export function teardown(data) {
  finishObservations(data);
}
