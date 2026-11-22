import { prepareAdmin, adminFlow, safeSystemTags } from './common.js';

const duration = __ENV.DURATION || '15m';
const vus = Number(__ENV.ADMIN_VUS || 3);

export const options = {
  systemTags: safeSystemTags,
  scenarios: {
    admin: {
      executor: 'constant-vus',
      vus,
      duration,
    },
  },
  thresholds: {
    'http_req_duration{scenario:admin}': ['p(95)<=2000'],
  },
};

export function setup() {
  return prepareAdmin();
}

export default function (data) {
  adminFlow(data);
}
