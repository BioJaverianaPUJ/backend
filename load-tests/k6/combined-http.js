import {
  adminFlow,
  mobileFlow,
  prepareCombined,
  safeSystemTags,
} from './common.js';

const duration = __ENV.DURATION || '15m';
const mobileVus = Number(__ENV.MOBILE_VUS || 50);
const adminVus = Number(__ENV.ADMIN_VUS || 3);

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
  },
  thresholds: {
    'http_req_duration{scenario:mobile}': ['p(95)<=1000'],
    'http_req_duration{scenario:admin}': ['p(95)<=2000'],
  },
};

export function setup() {
  return prepareCombined();
}

export function mobile(data) {
  mobileFlow(data);
}

export function admin(data) {
  adminFlow(data);
}
