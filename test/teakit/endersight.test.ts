import { Capability, Readiness, describe, expect, pos, test } from "@teakit/test";
import type { SpyCall, TeaKitTestContext } from "@teakit/test";

describe.configure({
  timeout: "8m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreenshot,
    Capability.PlayerSelf,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.SpyInstrumentation,
    Capability.WorldBlock,
    Capability.WorldEntities,
  ],
});

const KEY_LEFT_SHIFT = 340;
const Y = 180;

describe("Ender Sight", () => {
  test("Ender Spyglass teleports to the scoped spot and spends a pearl", async (ctx) => {
    const { commands, player, runtime } = ctx;
    const origin = await arena(ctx, 0);
    await commands.batch([
      "/gamemode survival @s",
      "/effect give @s minecraft:resistance 30 4 true",
      "/item replace entity @s weapon.mainhand with endersight:ender_spyglass",
      "/give @s minecraft:ender_pearl 2",
      `/fill ${origin.x + 14} ${Y} ${origin.z} ${origin.x + 14} ${Y + 3} ${origin.z + 4} minecraft:stone`,
    ]);
    await aim(ctx, { x: origin.x + 14, y: Y + 1.62, z: origin.z + 2.5 });

    // Scoping must last 10 server ticks before letting go teleports. A loaded server can tick far slower than the
    // client, so wait on the server clock.
    await player.holdUse(true);
    await waitServerTicks(ctx, 20);
    await player.holdUse(false);

    // Lands in front of the wall face it was scoping.
    await expect(async () => Math.abs((await player.position()).x - (origin.x + 13.5))).toEventuallyBeLessThan(0.1, { timeout: "3s" });
    expect(countOf(await player.inventory(), "minecraft:ender_pearl")).toBe(1);
  });

  test("Attuned Eye flies to the block it was attuned to", async (ctx) => {
    const { client, commands, player, runtime, entities } = ctx;
    const origin = await arena(ctx, 40);
    await commands.batch([
      "/gamemode creative @s",
      "/item replace entity @s weapon.mainhand with minecraft:ender_eye 2",
      `/setblock ${origin.x + 3} ${Y} ${origin.z + 2} minecraft:lodestone`,
    ]);
    const anchor = pos(origin.x + 3, Y, origin.z + 2);

    await client.keyState(KEY_LEFT_SHIFT, true);
    await runtime.wait(200);
    await player.lookAt(anchor);
    await player.useBlock(anchor);
    await client.keyState(KEY_LEFT_SHIFT, false);
    await runtime.wait(300);
    expect(countOf(await player.inventory(), "endersight:attuned_eye")).toBe(1);
    expect(countOf(await player.inventory(), "minecraft:ender_eye")).toBe(1);

    await commands.run("/item replace entity @s weapon.mainhand with minecraft:air");
    await commands.run("/clear @s minecraft:ender_eye");
    await player.inventory().selectHotbar(slotOf(await player.inventory(), "endersight:attuned_eye"));
    // Walk away first so the eye has a visible trip back to the lodestone.
    await commands.run(`/tp @s ${origin.x + 14.5} ${Y} ${origin.z + 2.5} 90 0`);
    await runtime.wait(300);
    await player.useItem();

    const eye = await entities.query({ origin: anchor, radius: 24, type: "minecraft:eye_of_ender" }).waitForCount(1, { timeout: "3s" });
    const start = (await eye[0].inspect()).position!;
    await runtime.wait(1_500);
    const later = (await eye[0].inspect()).position!;
    expect(distance(later, center(anchor))).toBeLessThan(distance(start, center(anchor)));
  });

  test("Everlasting Eye always drops back after its flight", async (ctx) => {
    const { commands, player, entities } = ctx;
    const origin = await arena(ctx, 80);
    const target = pos(origin.x + 8, Y, origin.z + 2);
    await commands.batch([
      "/gamemode creative @s",
      `/item replace entity @s weapon.mainhand with endersight:everlasting_eye[endersight:attunement={place:{dimension:"minecraft:overworld",pos:[I;${target.x},${target.y},${target.z}]}}]`,
    ]);
    await aim(ctx, center(target));
    await player.useItem();

    // Vanilla eyes live 80 ticks, then shatter or drop. Everlasting eyes must drop.
    const dropped = await entities.query({ origin: target, radius: 16, item: "endersight:everlasting_eye" })
      .waitForCount(1, { timeout: "8s" });
    expect(dropped.length).toBe(1);
  });

  test("Ender Mark marks an entity for the user and nearby mark carriers", async (ctx) => {
    const { commands, player, spy } = ctx;
    const origin = await arena(ctx, 120);
    await commands.batch([
      "/gamemode survival @s",
      `/summon minecraft:zombie ${origin.x + 8.5} ${Y} ${origin.z + 2.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
      "/item replace entity @s weapon.mainhand with endersight:ender_mark 2",
    ]);
    const carrier = await player.fake.spawn("MarkCarrier", pos(origin.x + 2, Y, origin.z + 4));
    const bystander = await player.fake.spawn("Bystander", pos(origin.x + 2, Y, origin.z));
    await commands.run(`/give ${carrier.name} endersight:ender_mark 1`);

    const marks = await spy.method("endersight.marks", "com.iamkaf.endersight.item.Marks#show");
    try {
      await aim(ctx, { x: origin.x + 8.5, y: Y + 1, z: origin.z + 2.5 });
      await player.useItem();
      const calls = await waitForCalls(ctx, marks.$calls, (all) => all.length >= 2);
      const viewers = calls.map((call) => JSON.stringify(call.args?.[0]));
      expect(viewers.some((viewer) => viewer.includes(carrier.name))).toBe(true);
      expect(viewers.some((viewer) => viewer.includes(bystander.name))).toBe(false);
      // Marks on mobs follow them: the anchor is the entity, not a point in the world.
      expect(calls.every((call) => JSON.stringify(call.args?.[1]).includes("entityId"))).toBe(true);
      expect(countOf(await player.inventory(), "endersight:ender_mark")).toBe(1);
      await ctx.client.screenshot("endersight-ender-mark", { hideOverlay: true });
    } finally {
      await marks.$detach();
    }
  });

  test("Ender Mark shows a marked player who is watching", async (ctx) => {
    const { commands, player, spy } = ctx;
    const origin = await arena(ctx, 140);
    await commands.batch([
      "/gamemode survival @s",
      "/item replace entity @s weapon.mainhand with endersight:ender_mark 1",
    ]);
    // Ender Marks share a 5-second cooldown, which an earlier mark test may have started.
    await waitServerTicks(ctx, 100);
    // Straight ahead of the player, so the mark doesn't depend on turning sideways.
    const watched = await player.fake.spawn("Watched", { x: origin.x + 6.5, y: Y, z: origin.z + 2.5 });
    // Spectators can't be marked; make sure the fake player is a normal target.
    await commands.run(`/gamemode survival ${watched.name}`);
    const spotted = await spy.method("endersight.spotted", "com.iamkaf.endersight.item.Marks#spotted");
    try {
      await aim(ctx, { x: watched.position.x, y: watched.position.y + 1, z: watched.position.z });
      await player.useItem();
      const calls = await waitForCalls(ctx, spotted.$calls, (all) => all.length >= 1);
      expect(calls.length).toBe(1);
      expect(JSON.stringify(calls[0].args?.[0])).toContain(watched.name);
    } finally {
      await spotted.$detach();
    }
  });

  test("Seer's Pearl marks the living things around where it lands", async (ctx) => {
    const { commands, player, spy } = ctx;
    const origin = await arena(ctx, 160);
    const landing = pos(origin.x + 5, Y - 1, origin.z + 2);
    await commands.batch([
      "/gamemode creative @s",
      `/summon minecraft:zombie ${landing.x + 1.5} ${Y} ${landing.z + 0.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
      `/summon minecraft:pig ${landing.x - 1.5} ${Y} ${landing.z + 1.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
      `/summon minecraft:zombie ${landing.x + 0.5} ${Y} ${landing.z - 1.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
      `/summon minecraft:zombie ${landing.x + 0.5} ${Y} ${landing.z + 20.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
      "/item replace entity @s weapon.mainhand with endersight:seers_pearl",
    ]);

    const marks = await spy.method("endersight.marks", "com.iamkaf.endersight.item.Marks#show");
    try {
      await aim(ctx, { x: landing.x + 0.5, y: Y, z: landing.z + 0.5 });
      await player.useItem();
      const calls = await waitForCalls(ctx, marks.$calls, (all) => all.length >= 3);
      await ctx.runtime.wait(500);
      expect((await marks.$calls()).length).toBe(3);
      expect(calls.length).toBe(3);
    } finally {
      await marks.$detach();
    }
  });

  test("Watcher's Eye powers redstone by distance and filters by mode", async (ctx) => {
    const { commands } = ctx;
    const origin = await arena(ctx, 200);
    const eye = pos(origin.x + 12, Y, origin.z + 2);
    await commands.batch([
      // Out of the eye's view, so only the zombie is in view.
      `/tp @s ${origin.x + 0.5} ${Y} ${origin.z + 6.5}`,
      `/setblock ${eye.x} ${eye.y} ${eye.z} endersight:watchers_eye[facing=west,mode=hostile]`,
      `/setblock ${eye.x + 1} ${eye.y} ${eye.z} minecraft:redstone_lamp`,
      `/summon minecraft:zombie ${eye.x - 2.5} ${Y} ${eye.z + 0.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`,
    ]);

    // The zombie stands 2.5 blocks out: 15, minus one per block past the first half block.
    await expect(() => powerOf(ctx, eye)).toEventuallyEqual("14", { timeout: "3s" });
    await commands.assert(`/execute if block ${eye.x + 1} ${eye.y} ${eye.z} minecraft:redstone_lamp[lit=true]`);

    await commands.run(`/setblock ${eye.x} ${eye.y} ${eye.z} endersight:watchers_eye[facing=west,mode=players,power=14]`);
    await expect(() => powerOf(ctx, eye)).toEventuallyEqual("0", { timeout: "3s" });
  });

  test("Watcher's Eye alerts the player who placed it", async (ctx) => {
    const { commands, player, spy } = ctx;
    const origin = await arena(ctx, 240);
    await commands.run("/gamemode creative @s");
    const alerts = await spy.method("endersight.alerts", "com.iamkaf.amber.api.functions.v1.PlayerFunctions#sendActionBarMessage");
    try {
      // Placed eyes look back at the placer, and new eyes watch for players.
      await player.place("endersight:watchers_eye", pos(origin.x + 4, Y, origin.z + 2));
      const calls = await waitForCalls(ctx, alerts.$calls, (all) => all.some(isWatcherAlert));
      expect(calls.some(isWatcherAlert)).toBe(true);
    } finally {
      await alerts.$detach();
    }
  });

  test("Ender Veil hides the player's gaze from Endermen", async (ctx) => {
    const { commands, player, runtime, spy } = ctx;
    const origin = await arena(ctx, 280);
    await commands.batch([
      "/gamemode survival @s",
      "/difficulty easy",
      "/time set 18000",
      "/effect give @s minecraft:resistance 60 4 true",
      "/item replace entity @s weapon.mainhand with endersight:ender_veil",
    ]);
    await player.holdUse(true);
    await player.waitForEffect("endersight:veiled", { timeout: "5s" });
    await player.holdUse(false);

    const gazes = await spy.method("endersight.veil", "com.iamkaf.endersight.item.VeiledEffect#hidesGaze");
    try {
      // A fence ring keeps the Enderman in place without blocking the line of sight between eyes.
      const cell = pos(origin.x + 6, Y, origin.z + 2);
      await commands.batch([
        `/fill ${cell.x - 1} ${Y} ${cell.z - 1} ${cell.x + 1} ${Y} ${cell.z + 1} minecraft:oak_fence`,
        `/setblock ${cell.x} ${Y} ${cell.z} minecraft:air`,
        `/summon minecraft:enderman ${cell.x + 0.5} ${Y} ${cell.z + 0.5} {Silent:1b,PersistenceRequired:1b}`,
      ]);
      const deadline = Date.now() + 10_000;
      let hidden = false;
      while (!hidden && Date.now() < deadline) {
        const enderman = await ctx.entities.nearest("minecraft:enderman", cell);
        const at = (await enderman!.inspect()).position!;
        await player.lookAt({ x: at.x, y: at.y + 2.55, z: at.z });
        await runtime.wait(250);
        hidden = (await gazes.$calls()).some((call) => call.returned === true);
      }
      expect(hidden).toBe(true);
    } finally {
      await gazes.$detach();
      await commands.run("/time set 6000");
    }
  });
});

async function waitServerTicks({ commands, runtime }: TeaKitTestContext, ticks: number) {
  const gameTime = async () => (await commands.run("/time query gametime")).result;
  const until = (await gameTime()) + ticks;
  while ((await gameTime()) < until) {
    await runtime.wait(100);
  }
}

/**
 * Turns the player's eyes toward a point on the server, so the server and client agree on the rotation before an item
 * is used. Client-only turns can reach the server late on a busy machine.
 */
async function aim({ commands, runtime }: TeaKitTestContext, target: { x: number; y: number; z: number }) {
  await commands.run(`/execute as @s at @s anchored eyes run rotate @s facing ${target.x} ${target.y} ${target.z}`);
  await runtime.wait(250);
}

/**
 * Builds a fenced glass platform at y=179 offset along x, clears air above it, and stands the player at its west edge
 * facing east. Returns the platform's north-west corner.
 */
async function arena({ client, commands, runtime }: TeaKitTestContext, offset: number) {
  const origin = pos(1000 + offset, Y, 0);
  await client.closeMenus();
  await commands.batch([
    `/forceload add ${origin.x - 16} ${origin.z - 16} ${origin.x + 40} ${origin.z + 40}`,
    `/kill @e[type=!minecraft:player,x=${origin.x - 4},y=${Y - 4},z=${origin.z - 8},dx=40,dy=12,dz=40]`,
    `/fill ${origin.x - 1} ${Y - 1} ${origin.z - 3} ${origin.x + 24} ${Y - 1} ${origin.z + 7} minecraft:glass`,
    `/fill ${origin.x - 1} ${Y} ${origin.z - 3} ${origin.x + 24} ${Y + 5} ${origin.z + 7} minecraft:air`,
    "/clear @s",
    `/tp @s ${origin.x + 0.5} ${Y} ${origin.z + 2.5} -90 0`,
  ]);
  await runtime.wait(500);
  await commands.assert(`/execute if block ${origin.x} ${Y - 1} ${origin.z + 2} minecraft:glass`);
  return origin;
}

async function waitForCalls(
  { runtime }: TeaKitTestContext,
  read: () => Promise<SpyCall[]>,
  done: (calls: SpyCall[]) => boolean,
  timeoutMs = 5_000,
): Promise<SpyCall[]> {
  const deadline = Date.now() + timeoutMs;
  let calls = await read();
  while (!done(calls) && Date.now() < deadline) {
    await runtime.wait(200);
    calls = await read();
  }
  return calls;
}

async function powerOf({ world }: TeaKitTestContext, at: { x: number; y: number; z: number }) {
  return (await world.block(pos(at.x, at.y, at.z))).properties?.power;
}

function isWatcherAlert(call: SpyCall): boolean {
  return JSON.stringify(call.args).includes("watchers_eye.alert");
}

/** The runtime reports `inventory[].item`, not the `items` list the SDK types describe. */
interface RuntimeInventory {
  inventory?: { slot: number | string; item?: { itemId: string; count: number } }[];
}

function stacks(inventory: unknown) {
  return ((inventory as RuntimeInventory).inventory ?? []).flatMap((entry) =>
    entry.item ? [{ slot: entry.slot, id: entry.item.itemId, count: entry.item.count }] : []
  );
}

function countOf(inventory: unknown, id: string): number {
  return stacks(inventory).filter((stack) => stack.id === id).reduce((sum, stack) => sum + stack.count, 0);
}

function slotOf(inventory: unknown, id: string): number {
  const slot = stacks(inventory).find((stack) => stack.id === id)?.slot;
  if (typeof slot !== "number" || slot > 8) throw new Error(`${id} is not in the hotbar`);
  return slot;
}

function center(at: { x: number; y: number; z: number }) {
  return { x: at.x + 0.5, y: at.y + 0.5, z: at.z + 0.5 };
}

function distance(a: { x: number; y: number; z: number }, b: { x: number; y: number; z: number }) {
  return Math.hypot(a.x - b.x, a.y - b.y, a.z - b.z);
}
