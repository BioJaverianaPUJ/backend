import http from 'k6/http';
import encoding from 'k6/encoding';
import { check, fail, sleep } from 'k6';
import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

const apiBaseUrl = (__ENV.API_BASE_URL || 'http://127.0.0.1:8000').replace(/\/$/, '');
const rabbitManagementUrl = (__ENV.RABBITMQ_MANAGEMENT_URL || 'http://127.0.0.1:15673').replace(/\/$/, '');

export const safeSystemTags = ['status', 'method', 'name', 'scenario', 'error_code', 'check'];
export const observationProcessingMs = new Trend('observation_processing_ms', true);
export const observationProcessingCompleted = new Rate('observation_processing_completed');
export const rabbitMessagesReady = new Gauge('rabbit_messages_ready');
export const rabbitMessagesUnacked = new Gauge('rabbit_messages_unacked');
export const rabbitMessagesTotal = new Gauge('rabbit_messages_total');
export const rabbitSamplingFailures = new Counter('rabbit_sampling_failures');

function assertLoopback(target, label) {
  if (typeof target !== 'string' || !target.trim()) {
    fail(`${label} must be a valid URL`);
  }

  const match = target.trim().match(
    /^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])(?::\d+)?(?:[/?#].*)?$/i
  );

  if (!match) {
    fail(`${label} must use localhost/loopback; refusing non-local target`);
  }
}


function requiredEnv(name) {
  const value = __ENV[name];
  if (!value) {
    fail(`Missing required environment variable: ${name}`);
  }
  return value;
}

function authHeaders(token) {
  return {
    Authorization: `Bearer ${token}`,
    Accept: 'application/json',
  };
}

function requestParams(token, name) {
  return {
    headers: authHeaders(token),
    tags: { name },
  };
}

function requireStatus(response, status, label) {
  const ok = check(response, {
    [`${label} returns HTTP ${status}`]: (res) => res.status === status,
  });
  if (!ok) {
    fail(`${label} returned HTTP ${response.status}`);
  }
}

function verifyRole(token, allowedRoles, label) {
  const response = http.get(`${apiBaseUrl}/api/v1/users/me`, {
    headers: authHeaders(token),
    tags: { name: `${label}_auth_preflight` },
  });
  requireStatus(response, 200, `${label} auth preflight`);

  let user;
  try {
    user = response.json();
  } catch (_) {
    fail(`${label} auth preflight returned invalid JSON`);
  }

  if (!allowedRoles.includes(user.role)) {
    fail(`${label} token must belong to role ${allowedRoles.join(' or ')}`);
  }
}

export function prepareMobile() {
  assertLoopback(apiBaseUrl, 'API_BASE_URL');
  const token = requiredEnv('MOBILE_TOKEN');
  verifyRole(token, ['USER'], 'mobile');
  return { mobileToken: token };
}

export function prepareAdmin() {
  assertLoopback(apiBaseUrl, 'API_BASE_URL');
  const token = requiredEnv('ADMIN_TOKEN');
  verifyRole(token, ['ADMIN', 'SUPERADMIN'], 'admin');
  return { adminToken: token };
}

export function prepareCombined() {
  assertLoopback(apiBaseUrl, 'API_BASE_URL');
  const mobileToken = requiredEnv('MOBILE_TOKEN');
  const adminToken = requiredEnv('ADMIN_TOKEN');
  verifyRole(mobileToken, ['USER'], 'mobile');
  verifyRole(adminToken, ['ADMIN', 'SUPERADMIN'], 'admin');
  return { mobileToken, adminToken };
}

function get(token, path, name) {
  const response = http.get(`${apiBaseUrl}${path}`, requestParams(token, name));
  requireStatus(response, 200, name);
}

function pause(minSeconds, maxSeconds) {
  sleep(minSeconds + Math.random() * (maxSeconds - minSeconds));
}

export function mobileFlow(data) {
  const token = data.mobileToken;

  get(token, '/api/v1/specimens?page=0&size=20', 'mobile_specimens');
  pause(1, 2.5);
  get(token, '/api/v1/posts?page=0&size=20', 'mobile_feed');
  pause(1.5, 3.5);
  get(token, '/api/v1/users/me', 'mobile_profile');
  pause(1, 2);
  get(token, '/api/v1/observations?page=0&size=20', 'mobile_observations');
  pause(1.5, 3);
}

export function adminFlow(data) {
  const token = data.adminToken;

  get(token, '/api/v1/species?page=0&size=20', 'admin_species_catalog');
  pause(1, 2);
  get(token, '/api/v1/users?page=0&size=20', 'admin_users');
  pause(1, 2);
  get(token, '/api/v1/reports/admin/all?page=0&size=20', 'admin_reports_all');
  pause(1, 2);
  get(token, '/api/v1/reports/admin/pending?page=0&size=20', 'admin_reports_pending');
  pause(1, 2);
  get(token, '/api/v1/posts/admin?page=0&size=20', 'admin_posts');
  pause(1, 2);
  get(token, '/api/v1/observations/admin?includeHidden=false&revisionRequested=false&page=0&size=20', 'admin_observations');
  pause(1, 2);
  get(token, '/api/v1/bans?page=0&size=20', 'admin_bans');
  pause(1.5, 3);
}

export function rabbitMonitorFlow() {
  sampleRabbitQueue('during');
  sleep(Number(__ENV.RABBITMQ_SAMPLE_INTERVAL_SEC || 1));
}

function rabbitQueueUrl() {
  const vhost = encodeURIComponent(__ENV.RABBITMQ_VHOST || '/');
  const queue = encodeURIComponent(__ENV.RABBITMQ_QUEUE || 'observation.jobs');
  return `${rabbitManagementUrl}/api/queues/${vhost}/${queue}`;
}

export function sampleRabbitQueue(phase) {
  assertLoopback(rabbitManagementUrl, 'RABBITMQ_MANAGEMENT_URL');
  const username = requiredEnv('RABBITMQ_USER');
  const password = requiredEnv('RABBITMQ_PASSWORD');
  const response = http.get(rabbitQueueUrl(), {
    headers: { Authorization: `Basic ${encoding.b64encode(`${username}:${password}`)}`, Accept: 'application/json' },
    tags: { name: 'rabbitmq_queue_metrics', phase },
  });

  const ok = check(response, {
    [`RabbitMQ queue metrics available (${phase})`]: (res) => res.status === 200,
  });
  if (!ok) {
    rabbitSamplingFailures.add(1, { phase });
    if (phase === 'before' || phase === 'after') {
      fail(`RabbitMQ management API returned HTTP ${response.status}`);
    }
    return null;
  }

  let queue;
  try {
    queue = response.json();
  } catch (_) {
    rabbitSamplingFailures.add(1, { phase });
    if (phase === 'before' || phase === 'after') {
      fail('RabbitMQ management API returned invalid JSON');
    }
    return null;
  }

  const ready = Number(queue.messages_ready);
  const unacked = Number(queue.messages_unacknowledged);
  const total = Number(queue.messages);
  rabbitMessagesReady.add(ready, { phase });
  rabbitMessagesUnacked.add(unacked, { phase });
  rabbitMessagesTotal.add(total, { phase });
  return { ready, unacked, total };
}

export function prepareObservations() {
  const data = prepareAdmin();
  assertLoopback(rabbitManagementUrl, 'RABBITMQ_MANAGEMENT_URL');
  requiredEnv('TEST_IMAGE_PATH');
  requiredEnv('TEST_IMAGE_CONTENT_TYPE');
  requiredEnv('PROCESSING_CONFIRM_TIMEOUT_SEC');
  requiredEnv('RABBITMQ_USER');
  requiredEnv('RABBITMQ_PASSWORD');

  const timeout = Number(__ENV.PROCESSING_CONFIRM_TIMEOUT_SEC);
  if (!Number.isFinite(timeout) || timeout <= 0) {
    fail('PROCESSING_CONFIRM_TIMEOUT_SEC must be a positive number');
  }

  data.processingTimeoutSeconds = timeout;
  data.rabbitBefore = sampleRabbitQueue('before');
  return data;
}

export function observationFlow(data, image) {
  const token = data.adminToken;
  const contentType = requiredEnv('TEST_IMAGE_CONTENT_TYPE');
  const latitude = Number(requiredEnv('TEST_LATITUDE'));
  const longitude = Number(requiredEnv('TEST_LONGITUDE'));

  const presignResponse = http.post(
    `${apiBaseUrl}/api/v1/observations/upload-presign`,
    JSON.stringify({ contentType }),
    {
      headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
      tags: { name: 'observation_upload_presign' },
    },
  );
  requireStatus(presignResponse, 200, 'observation upload presign');

  let presign;
  try {
    presign = presignResponse.json();
  } catch (_) {
    fail('Observation presign returned invalid JSON');
  }
  if (!presign.key || !presign.url || presign.method !== 'PUT') {
    fail('Observation presign response did not contain the expected upload fields');
  }
  assertLoopback(presign.url, 'presigned S3 URL');

  const uploadResponse = http.request(presign.method, presign.url, image, {
    headers: { 'Content-Type': presign.contentType || contentType },
    tags: { name: 'observation_s3_upload' },
  });
  requireStatus(uploadResponse, 200, 'test image upload');

  const createResponse = http.post(
    `${apiBaseUrl}/api/v1/observations`,
    JSON.stringify({
      referencePictureKey: presign.key,
      latitude,
      longitude,
      waitTimeoutSeconds: 0,
    }),
    {
      headers: { ...authHeaders(token), 'Content-Type': 'application/json' },
      tags: { name: 'observation_create' },
    },
  );
  requireStatus(createResponse, 201, 'observation create');

  let created;
  try {
    created = createResponse.json();
  } catch (_) {
    fail('Observation create returned invalid JSON');
  }
  const observationId = created.observation && created.observation.id;
  if (!observationId) {
    fail('Observation create response did not include observation.id');
  }

  const submittedAt = Date.now();
  let finalStatus = created.status;
  const deadline = Date.now() + data.processingTimeoutSeconds * 1000;
  while (finalStatus !== 'COMPLETED' && finalStatus !== 'FAILED' && Date.now() < deadline) {
    sleep(Number(__ENV.STATUS_POLL_INTERVAL_SEC || 2));
    const statusResponse = http.get(
      `${apiBaseUrl}/api/v1/observations/${observationId}/job-status`,
      requestParams(token, 'observation_job_status'),
    );
    requireStatus(statusResponse, 200, 'observation job status');

    try {
      finalStatus = statusResponse.json().status;
    } catch (_) {
      fail('Observation job status returned invalid JSON');
    }
  }

  const completed = finalStatus === 'COMPLETED';
  observationProcessingCompleted.add(completed);
  if (completed) {
    observationProcessingMs.add(Date.now() - submittedAt, { result: 'completed' });
  } else if (finalStatus === 'FAILED') {
    observationProcessingMs.add(Date.now() - submittedAt, { result: 'failed' });
  } else {
    observationProcessingMs.add(Date.now() - submittedAt, { result: 'not_completed_in_observation_window' });
  }
  check(finalStatus, {
    'observation reached a terminal processing state': (status) => status === 'COMPLETED' || status === 'FAILED',
    'observation processing completed': (status) => status === 'COMPLETED',
  });
}

export function finishObservations(data) {
  const after = sampleRabbitQueue('after');
  if (after && data.rabbitBefore) {
    check(after, {
      'RabbitMQ ready-message count did not finish above its baseline': (metrics) => metrics.ready <= data.rabbitBefore.ready,
      'RabbitMQ total-message count did not finish above its baseline': (metrics) => metrics.total <= data.rabbitBefore.total,
    });
  }
}
