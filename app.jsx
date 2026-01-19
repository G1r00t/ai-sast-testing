// tests/testcode/react_multi_hop/App.jsx
/**
 * Multi-hop attack path test project for React.
 *
 * This file intentionally contains:
 * - LIVE vulnerable paths
 * - DEAD code paths
 * - Sanitized but unsafe paths
 * - Context-confused sanitization
 * - Conditional sanitization
 * - Alias-based sinks
 * - Async sinks
 * - Cross-component callback taint
 * - Fake sanitizers
 *
 * Purpose: break naive SAST + remediation engines.
 */

import React, { useState, useEffect } from 'react';
import DOMPurify from 'dompurify';

// ==============================================================================
// SCENARIO A: Multi-hop LIVE path (JSX -> controller -> service -> sink)
// Expected: LIVE, classification=must_fix
// ==============================================================================

function renderUnsafeHTML(html) {
  document.getElementById('output').innerHTML = html;
}

function processUserContent(content) {
  const formatted = `<div class="user-content">${content}</div>`;
  return renderUnsafeHTML(formatted);
}

function handleContentSubmit(data) {
  return processUserContent(data.userContent);
}

export function VulnerableForm() {
  const [userContent, setUserContent] = useState('');

  const onSubmitClick = () => {
    handleContentSubmit({ userContent });
  };

  return (
    <div>
      <textarea value={userContent} onChange={(e) => setUserContent(e.target.value)} />
      <button onClick={onSubmitClick}>Submit (Unsafe)</button>
      <div id="output"></div>
    </div>
  );
}

// ==============================================================================
// SCENARIO B: Dead code
// Expected: DEAD
// ==============================================================================

function unusedDangerousSink(data) {
  return eval(data);
}

function NeverUsedComponent() {
  const [data, setData] = useState('');
  const handleClick = () => unusedDangerousSink(data);
  return <button onClick={handleClick}>Never Rendered</button>;
}

// ==============================================================================
// SCENARIO C: Sanitized path (correct)
// Expected: LIVE but sanitized
// ==============================================================================

function sanitizeHTML(html) {
  return DOMPurify.sanitize(html);
}

function renderSafeHTML(html) {
  document.getElementById('safe-output').innerHTML = html;
}

function handleSafeSubmit(data) {
  const safeHTML = sanitizeHTML(data.userContent);
  return renderSafeHTML(safeHTML);
}

export function SafeForm() {
  const [userContent, setUserContent] = useState('');

  return (
    <div>
      <textarea value={userContent} onChange={(e) => setUserContent(e.target.value)} />
      <button onClick={() => handleSafeSubmit({ userContent })}>Submit (Safe)</button>
      <div id="safe-output"></div>
    </div>
  );
}

// ==============================================================================
// SCENARIO D: dangerouslySetInnerHTML
// Expected: LIVE, must_fix
// ==============================================================================

function processTemplate(userData) {
  return `<span>Hello, ${userData}</span>`;
}

export function DangerousComponent() {
  const [name, setName] = useState('');
  const [html, setHtml] = useState('');

  useEffect(() => {
    if (name) setHtml(processTemplate(name));
  }, [name]);

  return (
    <div>
      <input value={name} onChange={(e) => setName(e.target.value)} />
      <div dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  );
}

// ==============================================================================
// SCENARIO E: Dynamic dispatch
// Expected: UNKNOWN
// ==============================================================================

function dynamicHandler(handlers, action, payload) {
  const handler = handlers[action];
  return handler ? handler(payload) : null;
}

function dangerousCallback(data) {
  return eval(data);
}

function safeCallback(data) {
  return JSON.stringify(data);
}

export function DynamicComponent() {
  const [action, setAction] = useState('safe');
  const [payload, setPayload] = useState('');

  const handlers = { dangerous: dangerousCallback, safe: safeCallback };

  return (
    <div>
      <select value={action} onChange={(e) => setAction(e.target.value)}>
        <option value="safe">Safe</option>
        <option value="dangerous">Dangerous</option>
      </select>
      <input onChange={(e) => setPayload(e.target.value)} />
      <button onClick={() => dynamicHandler(handlers, action, payload)}>Execute</button>
    </div>
  );
}

// ==============================================================================
// SCENARIO F: Fetch URL injection
// Expected: LIVE, must_fix
// ==============================================================================

function makeAPICall(url) {
  return fetch(url);
}

function buildAPIUrl(endpoint, params) {
  return `${endpoint}?${new URLSearchParams(params).toString()}`;
}

export function SearchComponent() {
  const [query, setQuery] = useState('');

  const onSearch = (e) => {
    e.preventDefault();
    makeAPICall(buildAPIUrl('/api/search', { q: query }));
  };

  return (
    <form onSubmit={onSearch}>
      <input onChange={(e) => setQuery(e.target.value)} />
      <button type="submit">Search</button>
    </form>
  );
}

// ==============================================================================
// SCENARIO G: Deep component chain
// Expected: LIVE
// ==============================================================================

function finalRender(data) {
  document.body.innerHTML += data;
}

function Level4({ data }) {
  useEffect(() => finalRender(data), [data]);
  return null;
}

const Level3 = ({ data }) => <Level4 data={data} />;
const Level2 = ({ data }) => <Level3 data={data} />;
const Level1 = ({ data }) => <Level2 data={data} />;

export function DeepComponent() {
  const [userData, setUserData] = useState('');
  return (
    <div>
      <input onChange={(e) => setUserData(e.target.value)} />
      <Level1 data={userData} />
    </div>
  );
}

// ==============================================================================
// SCENARIO H: Context-confused sanitization
// Expected: LIVE, must_fix
// ==============================================================================

function sanitizeHTMLOnly(input) {
  return DOMPurify.sanitize(input);
}

export function ContextConfusionComponent() {
  const [payload, setPayload] = useState('');

  const onClick = () => {
    const sanitized = sanitizeHTMLOnly(payload);
    eval(`console.log("${sanitized}")`);
  };

  return (
    <div>
      <input onChange={(e) => setPayload(e.target.value)} />
      <button onClick={onClick}>Context Confusion</button>
    </div>
  );
}

// ==============================================================================
// SCENARIO I: Conditional sanitization
// Expected: LIVE, must_fix
// ==============================================================================

function maybeSanitize(input, trusted) {
  return trusted ? DOMPurify.sanitize(input) : input;
}

export function ConditionalSanitizeComponent() {
  const [text, setText] = useState('');
  const [trusted, setTrusted] = useState(false);

  return (
    <div>
      <input onChange={(e) => setText(e.target.value)} />
      <label>
        <input type="checkbox" onChange={(e) => setTrusted(e.target.checked)} /> Trusted
      </label>
      <button onClick={() => document.body.innerHTML = maybeSanitize(text, trusted)}>
        Render
      </button>
    </div>
  );
}

// ==============================================================================
// SCENARIO J: Alias-based sink
// Expected: LIVE, must_fix
// ==============================================================================

const dangerousSink = document.body;

export function AliasSinkComponent() {
  const [data, setData] = useState('');
  return (
    <div>
      <input onChange={(e) => setData(e.target.value)} />
      <button onClick={() => dangerousSink.innerHTML = data}>Alias Sink</button>
    </div>
  );
}

// ==============================================================================
// SCENARIO K: Callback escaping component boundary
// Expected: LIVE, must_fix
// ==============================================================================

function Child({ onRender }) {
  useEffect(() => {
    onRender("<img src=x onerror=alert(1) />");
  }, []);
  return null;
}

export function CallbackEscapeComponent() {
  const renderHTML = (html) => {
    document.body.innerHTML += html;
  };
  return <Child onRender={renderHTML} />;
}

// ==============================================================================
// SCENARIO L: Async sink
// Expected: LIVE, must_fix
// ==============================================================================

function asyncRender(data) {
  return Promise.resolve(data).then((res) => {
    document.getElementById('async').innerHTML = res;
  });
}

export function AsyncComponent() {
  const [text, setText] = useState('');

  return (
    <div>
      <input onChange={(e) => setText(e.target.value)} />
      <button onClick={() => asyncRender(text)}>Async Render</button>
      <div id="async" />
    </div>
  );
}

// ==============================================================================
// SCENARIO M: Fake sanitizer
// Expected: LIVE, must_fix
// ==============================================================================

function sanitize(input) {
  return input.replace('script', '');
}

export function FakeSanitizerComponent() {
  const [data, setData] = useState('');

  return (
    <div>
      <input onChange={(e) => setData(e.target.value)} />
      <button onClick={() => document.body.innerHTML = sanitize(data)}>
        Fake Sanitize
      </button>
    </div>
  );
}

// ==============================================================================
// MAIN APP
// ==============================================================================

export default function App() {
  return (
    <div className="app">
      <h1>Multi-hop SAST Torture Suite</h1>

      <VulnerableForm />
      <SafeForm />
      <DangerousComponent />
      <DynamicComponent />
      <SearchComponent />
      <DeepComponent />

      <ContextConfusionComponent />
      <ConditionalSanitizeComponent />
      <AliasSinkComponent />
      <CallbackEscapeComponent />
      <AsyncComponent />
      <FakeSanitizerComponent />
    </div>
  );
}

