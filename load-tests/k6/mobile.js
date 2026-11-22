import { prepareMobile, mobileFlow, safeSystemTags } from './common.js';

const duration = __ENV.DURATION || '15m';
const vus = Number(__ENV.MOBILE_VUS || 50);

export const options = {
  systemTags: safeSystemTags,
  scenarios: {
    mobile: {
      executor: 'constant-vus',
      vus,
      duration,
    },
  },
  thresholds: {
    'http_req_duration{scenario:mobile}': ['p(95)<=1000'],
  },
};

export function setup() {
  return prepareMobile();
}

export default function (data) {
  mobileFlow(data);
}
