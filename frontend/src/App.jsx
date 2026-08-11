import { useEffect, useRef, useState } from "react";
import {
  ArrowUpRight,
  Camera,
  ChevronRight,
  Copy,
  Eye,
  EyeOff,
  Heart,
  Home,
  ImagePlus,
  KeyRound,
  LogOut,
  Mail,
  MessageCircleHeart,
  Pencil,
  Plus,
  Send,
  ShieldCheck,
  Sparkles,
  User,
  UserRound,
  UsersRound,
  X,
} from "lucide-react";

const localApiBase = typeof window === "undefined"
  ? "http://localhost:8080/api"
  : `${window.location.protocol}//${window.location.hostname}:8080/api`;
const API_BASE = import.meta.env.VITE_API_BASE_URL
  || (import.meta.env.DEV ? localApiBase : "/api");
const SERVER_BASE = API_BASE.replace(/\/api$/, "");

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function apiJson(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: "include",
    ...options,
    headers: {
      ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...options.headers,
    },
  });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(response.status, body?.message || "请求未能完成，请稍后重试");
  }
  return body;
}

function formatDate(dateValue) {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "long",
    day: "numeric",
  }).format(new Date(`${dateValue}T12:00:00`));
}

function formatTime(dateValue) {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(dateValue));
}

function imageUrl(url) {
  return url?.startsWith("/") ? `${SERVER_BASE}${url}` : url;
}

export default function App() {
  const [stage, setStage] = useState("loading");
  const [user, setUser] = useState(null);
  const [space, setSpace] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [activeView, setActiveView] = useState("home");
  const [dialog, setDialog] = useState(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  async function loadDashboard(nextSpace) {
    const data = await apiJson(`/dashboard?spaceId=${nextSpace.id}`);
    setSpace(data.space);
    setDashboard(data);
  }

  async function continueAfterAuth(nextUser) {
    setUser(nextUser);
    const spaces = await apiJson("/spaces");
    if (spaces.length === 0) {
      setStage("space");
      return;
    }
    await loadDashboard(spaces[0]);
    setStage("app");
  }

  async function bootstrap() {
    try {
      const currentUser = await apiJson("/auth/me");
      await continueAfterAuth(currentUser);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        setStage("auth");
        return;
      }
      setError(requestError.message);
      setStage("error");
    }
  }

  useEffect(() => {
    void bootstrap();
  }, []);

  async function refreshDashboard(message) {
    try {
      await loadDashboard(space);
      setNotice(message);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        setUser(null);
        setSpace(null);
        setDashboard(null);
        setStage("auth");
        return;
      }
      setNotice(requestError.message);
    }
  }

  async function handleSpaceReady(nextSpace) {
    try {
      await loadDashboard(nextSpace);
      setStage("app");
    } catch (requestError) {
      setError(requestError.message);
      setStage("error");
    }
  }

  async function logout() {
    try {
      await apiJson("/auth/logout", { method: "POST" });
    } catch {
      // Clear the local view even when an expired session cannot be deleted.
    }
    setUser(null);
    setSpace(null);
    setDashboard(null);
    setActiveView("home");
    setStage("auth");
  }

  if (stage === "loading") {
    return <div className="loading-screen">正在打开我们的故事...</div>;
  }

  if (stage === "error") {
    return (
      <div className="error-screen">
        <Heart fill="currentColor" />
        <h1>暂时没有打开这个空间</h1>
        <p>{error}</p>
        <button onClick={() => void bootstrap()}>再试一次</button>
      </div>
    );
  }

  if (stage === "auth") {
    return <AuthScreen onAuthenticated={continueAfterAuth} />;
  }

  if (stage === "space") {
    return <SpaceSetupScreen user={user} onSpaceReady={handleSpaceReady} />;
  }

  const latestMemory = dashboard.memories[0];
  const latestLetter = dashboard.letters[0];
  const hasPartner = space.members.length === 2;

  function openLetter() {
    if (!hasPartner) {
      setNotice("先把邀请码发给对方，加入后就可以写悄悄话了");
      return;
    }
    setDialog("letter");
  }

  async function copyInviteCode() {
    try {
      await navigator.clipboard.writeText(space.inviteCode);
      setNotice("邀请码已复制，发给对方即可加入");
    } catch {
      setNotice(`邀请码：${space.inviteCode}`);
    }
  }

  return (
    <main className="app-shell">
      <section className="mobile-frame">
        <header className="topbar">
          <div>
            <p className="content-kicker space-kicker">只属于两个人的地方</p>
            <h1>{space.name}</h1>
          </div>
          <div className="top-actions">
            <button className="avatar-stack" aria-label="查看成员">
              {space.members.map((member) => <span key={member.id}>{member.displayName.slice(0, 1)}</span>)}
            </button>
            <button className="top-icon-button" onClick={() => void logout()} aria-label="退出登录">
              <LogOut />
            </button>
          </div>
        </header>

        {activeView === "home" && (
          <HomeView
            space={space}
            memory={latestMemory}
            letter={latestLetter}
            hasPartner={hasPartner}
            onOpenDialog={setDialog}
            onEditRelationship={() => setDialog("relationship")}
            onOpenLetter={openLetter}
            onNavigate={setActiveView}
          />
        )}
        {activeView === "memories" && (
          <MemoriesView memories={dashboard.memories} onOpenDialog={setDialog} onEditMemory={(memory) => setDialog({ type: "memory", memory })} />
        )}
        {activeView === "letters" && (
          <LettersView letters={dashboard.letters} hasPartner={hasPartner} onOpenDialog={setDialog} onOpenLetter={openLetter} />
        )}
        {activeView === "stories" && (
          <StoriesView
            space={space}
            memories={dashboard.memories}
            hasPartner={hasPartner}
            onCopyInvite={copyInviteCode}
            onEditMemory={(memory) => setDialog({ type: "memory", memory })}
          />
        )}

        <nav className="bottom-nav">
          <NavButton active={activeView === "home"} icon={<Home />} label="此刻" onClick={() => setActiveView("home")} />
          <NavButton active={activeView === "memories"} icon={<Camera />} label="回忆" onClick={() => setActiveView("memories")} />
          <button className="create-button" onClick={() => setDialog("memory")} aria-label="记录回忆">
            <Plus />
          </button>
          <NavButton active={activeView === "letters"} icon={<Mail />} label="悄悄话" onClick={() => setActiveView("letters")} />
          <NavButton active={activeView === "stories"} icon={<Sparkles />} label="故事簿" onClick={() => setActiveView("stories")} />
        </nav>
      </section>

      {notice && <button className="notice" onClick={() => setNotice("")}>{notice}</button>}

      {dialog === "memory" && <MemoryDialog spaceId={space.id} onClose={() => setDialog(null)} onSuccess={refreshDashboard} />}
      {dialog === "relationship" && <RelationshipDateDialog space={space} onClose={() => setDialog(null)} onSuccess={refreshDashboard} />}
      {dialog && typeof dialog === "object" && dialog.type === "memory" && (
        <MemoryDialog spaceId={space.id} memory={dialog.memory} onClose={() => setDialog(null)} onSuccess={refreshDashboard} />
      )}
      {dialog === "letter" && <LetterDialog spaceId={space.id} onClose={() => setDialog(null)} onSuccess={refreshDashboard} />}
      {dialog && typeof dialog === "object" && dialog.type === "reply" && (
        <ReplyDialog spaceId={space.id} letter={dialog.letter} onClose={() => setDialog(null)} onSuccess={refreshDashboard} />
      )}
    </main>
  );
}

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ displayName: "", username: "", password: "" });
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  function update(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function changeMode(nextMode) {
    setMode(nextMode);
    setMessage("");
    setShowPassword(false);
  }

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    try {
      const path = mode === "login" ? "/auth/login" : "/auth/register";
      const body = mode === "login"
        ? { username: form.username, password: form.password }
        : form;
      const nextUser = await apiJson(path, { method: "POST", body: JSON.stringify(body) });
      await onAuthenticated(nextUser);
    } catch (requestError) {
      setMessage(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-frame">
        <div className="auth-illustration">
          <div className="auth-brand">
            <span className="brand-heart"><Heart fill="currentColor" /></span>
            <p>我们之间</p>
          </div>
          <div className="auth-orbit" aria-hidden="true">
            <span className="orbit-star one">+</span>
            <span className="orbit-star two">+</span>
            <span className="orbit-star three">+</span>
            <div className="auth-people">
              <span className="person-card warm"><User /></span>
              <span className="heart-link"><Heart fill="currentColor" /></span>
              <span className="person-card green"><User /></span>
            </div>
          </div>
          <h1>远一点，也在彼此身边。</h1>
          <span>照片、故事和那些不好意思当面说的话，只留给你们两个人。</span>
        </div>
        <section className="auth-panel">
          <div className="auth-tabs" role="tablist">
            <button type="button" className={mode === "login" ? "active" : ""} onClick={() => changeMode("login")}>回到这里</button>
            <button type="button" className={mode === "register" ? "active" : ""} onClick={() => changeMode("register")}>第一次来</button>
          </div>
          <div className="auth-heading">
            <p className="eyebrow">{mode === "login" ? "WELCOME BACK" : "TWO PEOPLE, ONE SPACE"}</p>
            <h2>{mode === "login" ? "回来看看我们的故事" : "先为自己留一个位置"}</h2>
            <p>{mode === "login" ? "输入你自己的身份和私密口令，继续进入你们的角落。" : "只需创建一次身份，之后用邀请码和对方相遇。"}</p>
          </div>
          <form className="auth-form" onSubmit={submit}>
            {mode === "register" && (
              <label className="input-field">
                <span><UserRound /> 你的昵称</span>
                <input value={form.displayName} onChange={(event) => update("displayName", event.target.value)} placeholder="例如：小满" maxLength="40" autoComplete="nickname" required />
              </label>
            )}
            <label className="input-field">
              <span><User /> 你的账号</span>
              <input value={form.username} onChange={(event) => update("username", event.target.value)} placeholder="4-24 位字母、数字或下划线" autoComplete="username" required />
            </label>
            <label className="input-field">
              <span><KeyRound /> 私密口令</span>
              <div className="password-control">
                <input type={showPassword ? "text" : "password"} value={form.password} onChange={(event) => update("password", event.target.value)} placeholder={mode === "login" ? "输入你设置的口令" : "至少 8 位，只有你知道"} autoComplete={mode === "login" ? "current-password" : "new-password"} minLength="8" required />
                <button type="button" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? "隐藏口令" : "显示口令"}>
                  {showPassword ? <EyeOff /> : <Eye />}
                </button>
              </div>
            </label>
            {message && <p className="form-message">{message}</p>}
            <button className="submit-button auth-submit" disabled={submitting}>
              {submitting ? "正在打开..." : mode === "login" ? "进入我们的空间" : "创建我的身份"}
              {!submitting && <ArrowUpRight />}
            </button>
          </form>
        </section>
      </section>
    </main>
  );
}

function SpaceSetupScreen({ user, onSpaceReady }) {
  const [mode, setMode] = useState("create");
  const [form, setForm] = useState({
    name: `${user.displayName}的日子`,
    relationshipStartedOn: new Date().toISOString().slice(0, 10),
    inviteCode: "",
  });
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSubmitting(true);
    setMessage("");
    try {
      const nextSpace = mode === "create"
        ? await apiJson("/spaces", {
          method: "POST",
          body: JSON.stringify({ name: form.name, relationshipStartedOn: form.relationshipStartedOn }),
        })
        : await apiJson("/spaces/join", {
          method: "POST",
          body: JSON.stringify({ inviteCode: form.inviteCode }),
        });
      await onSpaceReady(nextSpace);
    } catch (requestError) {
      setMessage(requestError.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="setup-frame">
        <div className="setup-mark"><Heart fill="currentColor" /></div>
        <p className="eyebrow">你好，{user.displayName}</p>
        <h1>{mode === "create" ? "从一页新故事开始" : "输入属于你们的邀请码"}</h1>
        <p>{mode === "create" ? "创建后，把邀请码发给对方。这个空间只容纳你们两个人。" : "加入后，你就可以看到并一起写下你们的故事。"}</p>
        <div className="setup-tabs">
          <button type="button" className={mode === "create" ? "active" : ""} onClick={() => { setMode("create"); setMessage(""); }}>创建空间</button>
          <button type="button" className={mode === "join" ? "active" : ""} onClick={() => { setMode("join"); setMessage(""); }}>加入空间</button>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {mode === "create" ? (
            <>
              <label className="input-field">
                <span><Heart /> 空间名字</span>
                <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="例如：小满与阿川的日子" maxLength="80" required />
              </label>
              <label className="input-field">
                <span><Sparkles /> 关系开始日期</span>
                <input type="date" value={form.relationshipStartedOn} onChange={(event) => setForm({ ...form, relationshipStartedOn: event.target.value })} required />
              </label>
            </>
          ) : (
            <label className="input-field invite-input">
              <span><UsersRound /> 邀请码</span>
              <input value={form.inviteCode} onChange={(event) => setForm({ ...form, inviteCode: event.target.value.toUpperCase() })} placeholder="例如：ABCD2345" maxLength="8" required />
            </label>
          )}
          {message && <p className="form-message">{message}</p>}
          <button className="submit-button auth-submit" disabled={submitting}>
            {submitting ? "正在准备..." : mode === "create" ? "创建我们的空间" : "进入这个空间"}
            {!submitting && <ArrowUpRight />}
          </button>
        </form>
        <p className="setup-note"><ShieldCheck /> 一个空间最多两个人，邀请码只发给值得一起收藏故事的人。</p>
      </section>
    </main>
  );
}

function HomeView({ space, memory, letter, hasPartner, onOpenDialog, onEditRelationship, onOpenLetter, onNavigate }) {
  return (
    <div className="screen-content home-view">
      <section className="day-count">
        <div>
          <p>我们已经一起走过</p>
          <strong>{space.daysTogether}</strong>
          <span> 天</span>
        </div>
        <div className="day-count-actions">
          <Heart fill="currentColor" />
          <button className="day-count-edit" onClick={onEditRelationship} aria-label="修改关系开始日期">
            <Pencil />
          </button>
        </div>
      </section>

      <section className="section-heading">
        <div>
          <p className="content-kicker">今日回忆</p>
          <h2>把小事，好好留下来</h2>
        </div>
        <button className="icon-button" onClick={() => onNavigate("memories")} aria-label="查看全部回忆">
          <ArrowUpRight />
        </button>
      </section>

      <article className="featured-memory">
        {memory?.imageUrl ? <img src={imageUrl(memory.imageUrl)} alt="" /> : <div className="featured-placeholder"><Heart fill="currentColor" /></div>}
        <div className="photo-glow" />
        <div className="memory-copy">
          <p>{memory ? formatDate(memory.occurredOn) : "今天"}</p>
          <h3>{memory?.title || "等你写下第一段故事"}</h3>
          <span>{memory?.content || "一张照片、一句心里话，都可以成为你们共同的记忆。"}</span>
        </div>
      </article>

      <section className="quick-actions">
        <button onClick={() => onOpenDialog("memory")}>
          <ImagePlus />
          <span>记录一件小事</span>
        </button>
        <button onClick={onOpenLetter} disabled={!hasPartner}>
          <MessageCircleHeart />
          <span>{hasPartner ? "写一段悄悄话" : "等待对方加入"}</span>
        </button>
      </section>

      <section className="letter-preview">
        <div className="section-heading compact">
          <div>
            <p className="content-kicker">悄悄话信箱</p>
            <h2>有些话，慢慢写</h2>
          </div>
          <button className="icon-button" onClick={() => onNavigate("letters")} aria-label="查看悄悄话">
            <ChevronRight />
          </button>
        </div>
        {letter ? (
          <button className="letter-card" onClick={() => onOpenDialog({ type: "reply", letter })}>
            <div className="letter-seal"><Heart fill="currentColor" /></div>
            <div>
              <p>来自 {letter.senderName}</p>
              <span>{letter.content}</span>
            </div>
            <ChevronRight />
          </button>
        ) : <p className="empty-copy">等一封想被好好读完的信。</p>}
      </section>
    </div>
  );
}

function MemoriesView({ memories, onOpenDialog, onEditMemory }) {
  return (
    <div className="screen-content list-view">
      <section className="list-title">
        <p className="content-kicker">回忆相册</p>
        <h2>被认真收藏的日常</h2>
        <button className="plain-action" onClick={() => onOpenDialog("memory")}><Plus /> 新增</button>
      </section>
      {memories.length === 0 ? <EmptyState icon={<Camera />} text="从第一张照片开始，留下你们的故事。" /> : (
        <div className="memory-grid">
          {memories.map((memory) => (
            <article className="memory-tile" key={memory.id}>
              {memory.imageUrl ? <img src={imageUrl(memory.imageUrl)} alt="" /> : <div className="memory-image-placeholder"><Camera /></div>}
              <div className="memory-tile-copy">
                <p>{formatDate(memory.occurredOn)} · {memory.authorName}</p>
                <h3>{memory.title}</h3>
                <span>{memory.content}</span>
                <button className="memory-edit-button" onClick={() => onEditMemory(memory)}><Pencil /> 编辑</button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

function LettersView({ letters, hasPartner, onOpenDialog, onOpenLetter }) {
  return (
    <div className="screen-content list-view">
      <section className="list-title">
        <p className="content-kicker">悄悄话信箱</p>
        <h2>把没说出口的话，交给文字</h2>
        <button className="plain-action" onClick={onOpenLetter} disabled={!hasPartner}><Send /> 写信</button>
      </section>
      {letters.length === 0 ? <EmptyState icon={<Mail />} text={hasPartner ? "写下第一封悄悄话吧。" : "等对方加入后，信箱会在这里慢慢装满。"} /> : (
        <div className="letters-list">
          {letters.map((letter) => (
            <button className="full-letter" onClick={() => onOpenDialog({ type: "reply", letter })} key={letter.id}>
              <div className="letter-meta">
                <span>来自 {letter.senderName}</span>
                <time>{formatTime(letter.createdAt)}</time>
              </div>
              <p>{letter.content}</p>
              <div className="letter-status">
                <span>{letter.replies.length ? `已有 ${letter.replies.length} 条回应` : "写下你的回应"}</span>
                <ChevronRight />
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function StoriesView({ space, memories, hasPartner, onCopyInvite, onEditMemory }) {
  return (
    <div className="screen-content list-view story-view">
      <section className="list-title">
        <p className="content-kicker">我们的故事簿</p>
        <h2>每一次靠近，都有迹可循</h2>
      </section>
      {!hasPartner && (
        <button className="invite-card story-invite-card" onClick={() => void onCopyInvite()}>
          <span><UsersRound /> 把邀请码分享给 TA</span>
          <strong>{space.inviteCode}</strong>
          <Copy />
        </button>
      )}
      {memories.length === 0 ? <EmptyState icon={<Sparkles />} text="第一页故事，等着你们一起写下。" /> : (
        <div className="timeline">
          {memories.map((memory) => (
            <article className="timeline-item" key={memory.id}>
              <div className="timeline-pin"><Heart fill="currentColor" /></div>
              <div>
                <time>{formatDate(memory.occurredOn)}</time>
                <h3>{memory.title}</h3>
                <p>{memory.content}</p>
                <button onClick={() => onEditMemory(memory)}>编辑这一页 <Pencil /></button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

function EmptyState({ icon, text }) {
  return <div className="empty-state">{icon}<p>{text}</p></div>;
}

function NavButton({ active, icon, label, onClick }) {
  return (
    <button className={`nav-button ${active ? "active" : ""}`} onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

function MemoryDialog({ spaceId, memory, onClose, onSuccess }) {
  const isEditing = Boolean(memory);
  const [form, setForm] = useState(() => ({
    title: memory?.title || "",
    content: memory?.content || "",
    occurredOn: memory?.occurredOn || new Date().toISOString().slice(0, 10),
    imageUrl: memory?.imageUrl || "",
  }));
  const [saving, setSaving] = useState(false);
  const fileRef = useRef(null);

  function update(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function uploadFile(file) {
    try {
      const upload = new FormData();
      upload.append("file", file);
      const result = await apiJson(`/uploads?spaceId=${spaceId}`, { method: "POST", body: upload });
      update("imageUrl", result.url);
    } catch (requestError) {
      onSuccess(requestError.message);
    }
  }

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await apiJson(isEditing ? `/memories/${memory.id}?spaceId=${spaceId}` : `/memories?spaceId=${spaceId}`, {
        method: isEditing ? "PATCH" : "POST",
        body: JSON.stringify(form),
      });
      onClose();
      await onSuccess(isEditing ? "这条回忆已更新" : "新的回忆已经收进故事簿");
    } catch (requestError) {
      onSuccess(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title={isEditing ? "编辑这段回忆" : "记录一件小事"} onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>标题<input value={form.title} onChange={(event) => update("title", event.target.value)} placeholder="例如：下雨天的一把伞" required /></label>
        <label>那天的故事<textarea value={form.content} onChange={(event) => update("content", event.target.value)} placeholder="写下你想记住的片段..." required rows="4" /></label>
        <label>发生日期<input type="date" value={form.occurredOn} onChange={(event) => update("occurredOn", event.target.value)} required /></label>
        <input ref={fileRef} className="visually-hidden" type="file" accept="image/*" onChange={(event) => event.target.files[0] && void uploadFile(event.target.files[0])} />
        <button className="upload-field" type="button" onClick={() => fileRef.current?.click()}>
          {form.imageUrl ? <><Camera /> {isEditing ? "更换照片" : "已选择照片"}</> : <><ImagePlus /> 添加一张照片</>}
        </button>
        <button className="submit-button" disabled={saving}>{saving ? "正在保存..." : isEditing ? "保存修改" : "收藏这段回忆"}</button>
      </form>
    </Dialog>
  );
}

function RelationshipDateDialog({ space, onClose, onSuccess }) {
  const [relationshipStartedOn, setRelationshipStartedOn] = useState(space.relationshipStartedOn);
  const [saving, setSaving] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await apiJson(`/spaces/${space.id}`, {
        method: "PATCH",
        body: JSON.stringify({ relationshipStartedOn }),
      });
      onClose();
      await onSuccess("关系开始日期已更新");
    } catch (requestError) {
      onSuccess(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title="修改关系开始日期" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>关系开始日期<input type="date" value={relationshipStartedOn} onChange={(event) => setRelationshipStartedOn(event.target.value)} required /></label>
        <button className="submit-button" disabled={saving}>{saving ? "正在保存..." : "保存日期"}</button>
      </form>
    </Dialog>
  );
}

function LetterDialog({ spaceId, onClose, onSuccess }) {
  const [content, setContent] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await apiJson(`/letters?spaceId=${spaceId}`, {
        method: "POST",
        body: JSON.stringify({ content }),
      });
      onClose();
      await onSuccess("这段话已经悄悄送出");
    } catch (requestError) {
      onSuccess(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title="写一段悄悄话" onClose={onClose}>
      <form className="form" onSubmit={submit}>
        <label>想说的话<textarea value={content} onChange={(event) => setContent(event.target.value)} placeholder="这一次，不用急着说出口..." required rows="7" /></label>
        <button className="submit-button" disabled={saving}>{saving ? "正在送出..." : "送出悄悄话"}</button>
      </form>
    </Dialog>
  );
}

function ReplyDialog({ spaceId, letter, onClose, onSuccess }) {
  const [content, setContent] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setSaving(true);
    try {
      await apiJson(`/letters/${letter.id}/replies?spaceId=${spaceId}`, {
        method: "POST",
        body: JSON.stringify({ content }),
      });
      onClose();
      await onSuccess("你的回应已经被好好收到");
    } catch (requestError) {
      onSuccess(requestError.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title={`来自 ${letter.senderName} 的悄悄话`} onClose={onClose}>
      <article className="dialog-letter">
        <p>{letter.content}</p>
        {letter.replies.map((reply) => <div key={reply.id}><strong>{reply.authorName}</strong>{reply.content}</div>)}
      </article>
      <form className="form" onSubmit={submit}>
        <label>写下回应<textarea value={content} onChange={(event) => setContent(event.target.value)} required placeholder="让对方知道你收到了..." rows="4" /></label>
        <button className="submit-button" disabled={saving}>{saving ? "正在回应..." : "温柔地回应"}</button>
      </form>
    </Dialog>
  );
}

function Dialog({ title, children, onClose }) {
  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="dialog" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()}>
        <header>
          <h2>{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="关闭"><X /></button>
        </header>
        {children}
      </section>
    </div>
  );
}
