"""One-time exporter: old TGItems numbers -> data/techguns3/guns/*.json (nested schema)."""
import json
from pathlib import Path

MOD = "techguns3"
out = Path(__file__).resolve().parents[1] / "src/main/resources/data/techguns3/guns"

def gun(ammo, empty, damage, cooldown, mag, reload, ttl, spread, speed, gravity,
        pellets, pellet_spread, drop_start, drop_end, drop_min, pen, semi, magfed,
        fire, reload_snd, extra_snd, projectile, can_zoom, zoom_fov, zoom_spread,
        recoil_pitch, recoil_yaw, muzzle, spin, slime, action_snd, action_delay, bulk,
        explosive=None, beam=False, charge=0, ammo_base=1, ammo_full=1,
        knockback=0.0, ignore_iframes=False, homing=0.0, guided=False,
        lock_time=20, lock_persist=80, lock_range=150.0, radiation=0,
        extra_ammo=None, flash=0, loop=0, continuous=False, two_handed=True):
    d = {"schema_version": 1, "ammo": f"{MOD}:{ammo}"}
    if empty: d["empty_magazine"] = f"{MOD}:{empty}"
    d["ballistic"] = {
        "damage": damage, "cooldown_ticks": cooldown, "magazine": mag,
        "reload_ticks": reload, "range_ttl": ttl, "spread": spread,
        "speed": speed, "gravity": gravity, "pellets": pellets,
        "pellet_spread": pellet_spread, "drop_start": drop_start,
        "drop_end": drop_end, "drop_min": drop_min, "penetration": pen,
        "semi_auto": semi, "magazine_fed": magfed,
    }
    sounds = {}
    if fire: sounds["fire"] = f"{MOD}:{fire}"
    if reload_snd: sounds["reload"] = f"{MOD}:{reload_snd}"
    if extra_snd: sounds["extra"] = f"{MOD}:{extra_snd}"
    if action_snd: sounds["action"] = f"{MOD}:{action_snd}"
    if action_delay: sounds["action_delay_ticks"] = action_delay
    if sounds: d["sounds"] = sounds
    d["handling"] = {
        "projectile": projectile, "can_zoom": can_zoom, "zoom_fov": zoom_fov,
        "zoom_spread_mult": zoom_spread, "recoil_pitch": recoil_pitch,
        "recoil_yaw": recoil_yaw, "muzzle_scale": muzzle,
        "spins_barrels": spin, "slime_per_hit": slime, "bulk": bulk,
        "muzzle_flash": flash, "loop_ticks": loop, "two_handed": two_handed,
    }
    extras = {}
    if explosive: extras["explosive"] = explosive
    if beam: extras["beam"] = True
    if charge: extras.update({"charge_ticks": charge, "ammo_base": ammo_base, "ammo_full": ammo_full})
    if knockback: extras["knockback"] = knockback
    if ignore_iframes: extras["ignore_iframes"] = True
    if homing: extras["homing_turn"] = homing
    if guided:
        extras.update({"guided": True, "lock_time": lock_time,
                       "lock_persist": lock_persist, "lock_range": lock_range})
    if radiation: extras["radiation_ticks"] = radiation
    if extra_ammo: extras["extra_ammo"] = [f"{MOD}:{a}" for a in extra_ammo]
    if continuous: extras["continuous_beam"] = True
    if extras: d["extras"] = extras
    return d

def exp(mx, mn, r1, r2, block=0.5, nuke=False):
    return {"max": mx, "min": mn, "r1": r1, "r2": r2, "block": block, "nuke": nuke}

GUNS = {
 "pistol": gun("pistol_magazine","pistol_magazine_empty",8.0,4,18,35,40,0.025,2.0,0.0,1,0.0,18.0,25.0,5.0,0.0,True,True,"pistol_fire","pistol_reload",None,"BALLISTIC",True,0.85,0.5,0.7,0.3,0.55,False,0.0,None,0,0.85,flash=2,two_handed=False),
 "revolver": gun("pistol_rounds",None,8.0,6,6,45,40,0.025,2.0,0.0,1,0.0,12.0,20.0,6.0,0.0,True,False,"revolver_fire","revolver_reload",None,"BALLISTIC",True,0.85,0.5,1.0,0.4,0.6,False,0.0,None,0,0.9,flash=2,two_handed=False),
 "ak47": gun("assault_rifle_magazine","assault_rifle_magazine_empty",8.0,3,30,45,60,0.030,4.0,0.0,1,0.0,20.0,30.0,4.5,0.5,False,True,"ak47_fire","ak47_reload",None,"BALLISTIC",True,0.7,0.5,0.9,0.4,0.8,False,0.0,None,0,1.0,flash=1),
 "m4": gun("assault_rifle_magazine","assault_rifle_magazine_empty",11.0,3,30,45,60,0.015,4.0,0.0,1,0.0,25.0,40.0,7.0,0.5,False,True,"m4_fire","m4_reload",None,"BALLISTIC",True,0.7,0.45,0.7,0.3,0.75,False,0.0,None,0,1.0,flash=1),
 "thompson": gun("smg_magazine","smg_magazine_empty",5.0,3,20,40,40,0.050,1.75,0.0,1,0.0,15.0,24.0,3.0,0.0,False,True,"thompson_fire","thompson_reload",None,"BALLISTIC",True,0.8,0.5,0.6,0.35,0.6,False,0.0,None,0,1.0,flash=1),
 "combat_shotgun": gun("shotgun_rounds",None,8.0,14,8,50,15,0.010,2.5,0.0,8,0.150,2.0,5.0,3.0,0.5,True,False,"combat_shotgun_fire","combat_shotgun_reload",None,"BALLISTIC",True,0.8,0.5,2.8,0.8,0.9,False,0.0,"combat_shotgun_pump",14,1.7,flash=2),
 "minigun": gun("minigun_drum","minigun_drum_empty",12.0,1,200,100,75,0.025,3.0,0.0,1,0.0,30.0,50.0,7.0,0.5,False,True,"minigun_fire","minigun_reload",None,"BALLISTIC",True,0.85,0.6,0.3,0.2,0.8,True,0.0,None,0,1.05,flash=3),
 "teslagun": gun("energy_cell","energy_cell_empty",9.0,8,25,45,60,0.0,80.0,0.0,1,0.0,0.0,0.0,9.0,0.0,False,True,"teslagun_fire","teslagun_reload",None,"ELECTRIC",True,0.8,0.5,0.4,0.1,0.6,False,0.09,None,0,1.0,flash=6),
 "bolt_action": gun("rifle_rounds",None,30.0,25,6,50,90,0.050,6.5,0.0,1,0.0,40.0,60.0,20.0,1.0,True,False,"bolt_action_fire","bolt_action_reload",None,"BALLISTIC",True,0.35,0.25,2.2,0.5,0.9,False,0.0,"bolt_action_rechamber",20,1.15,flash=2),
 "biogun": gun("bio_tank","bio_tank_empty",25.0,6,30,45,40,0.015,1.0,0.01,1,0.0,8.0,15.0,20.0,0.5,False,True,"biogun_fire","biogun_reload",None,"BIO",True,0.8,0.5,0.8,0.3,0.7,False,0.34,None,0,1.6,flash=11),
 "flamethrower": gun("fuel_tank","fuel_tank_empty",8.0,2,100,45,22,0.05,0.55,0.01,1,0.0,5.0,22.0,4.0,0.0,False,True,"flamethrower_fire","flamethrower_reload","flamethrower_start","FIRE",True,0.8,0.6,0.5,0.25,0.8,False,0.0,None,0,1.3,flash=10,loop=10),
 "tfg": gun("nuclear_powercell","nuclear_powercell_empty",70.0,12,20,45,17,0.015,6.0,0.0,1,0.0,8.0,15.0,30.0,2.0,False,True,"tfg_fire","biogun_reload","tfg_charge","PLASMA",True,0.8,0.5,1.2,0.4,1.0,False,0.0,None,0,2.0,explosive=exp(46.0,23.0,5.0,10.0,0.5),charge=60,ammo_base=1,ammo_full=5,flash=9),
 "nucleardeathray": gun("nuclear_powercell","nuclear_powercell_empty",5.0,5,20,50,100,0.0,1.0,0.0,1,0.0,11.0,11.0,5.0,1.0,False,True,"ndr_fire","ndr_reload","ndr_start","BEAM",True,0.8,0.5,0.4,0.1,0.8,False,0.0,None,0,1.0,beam=True,radiation=40,flash=8,loop=10,continuous=True),
 "gaussrifle": gun("gauss_magazine","gauss_magazine_empty",90.0,30,8,60,18,0.025,5.0,0.0,1,0.0,90.0,90.0,90.0,3.5,True,True,"gauss_fire","gauss_reload",None,"GAUSS",True,0.35,0.0,2.4,0.5,0.9,False,0.0,"gauss_rechamber",12,1.1,flash=7),
 "rocketlauncher": gun("rocket",None,60.0,10,1,40,200,0.05,1.0,0.01,1,0.0,3.0,5.0,50.0,0.0,True,False,"rocket_fire","rocket_reload",None,"EXPLOSIVE",True,0.8,0.6,2.0,0.5,1.0,False,0.0,None,0,2.2,explosive=exp(60.0,50.0,3.0,5.0,0.5),knockback=3.0,extra_ammo=["rocket_nuke","rocket_hv"],flash=2),
 "guidedmissilelauncher": gun("rocket",None,60.0,10,1,40,100,0.05,1.0,0.01,1,0.0,2.0,4.0,50.0,0.0,True,False,"guided_fire","rocket_reload",None,"EXPLOSIVE",True,0.8,0.6,2.0,0.5,1.0,False,0.0,None,0,2.2,explosive=exp(60.0,50.0,2.0,4.0,0.25),knockback=2.0,homing=0.16,guided=True,lock_time=20,lock_persist=80,lock_range=150.0,extra_ammo=["rocket_hv"],flash=2),
 "grimreaper": gun("rocket",None,60.0,6,4,40,200,0.05,1.0,0.01,1,0.0,3.0,5.0,50.0,0.0,False,False,"guided_fire","rocket_reload",None,"EXPLOSIVE",True,0.8,0.6,1.6,0.4,1.0,False,0.0,None,0,2.2,explosive=exp(60.0,50.0,3.0,5.0,0.25),knockback=2.0,homing=0.16,guided=True,lock_time=20,lock_persist=80,lock_range=150.0,extra_ammo=["rocket_hv"],flash=2),
 "sonicshotgun": gun("energy_cell","energy_cell_empty",50.0,12,8,40,20,0.0,2.0,0.0,16,0.075,5.0,15.0,35.0,1.0,True,True,"sonic_fire","sonic_reload",None,"SONIC",True,0.8,0.6,2.4,0.7,0.9,False,0.0,None,0,1.4,knockback=2.0,ignore_iframes=True,flash=7),
 "lasergun": gun("energy_cell","energy_cell_empty",16.0,5,45,45,90,0.0,1.0,0.0,1,0.0,90.0,90.0,16.0,0.0,False,True,"laser_fire","laser_reload",None,"LASER",True,0.75,0.75,0.5,0.15,0.7,False,0.0,None,0,1.0,beam=True,flash=5),
 "laserpistol": gun("redstone_battery","redstone_battery_empty",9.0,6,20,40,90,0.025,1.0,0.0,1,0.0,90.0,90.0,9.0,0.0,False,True,"laserpistol_fire","laserpistol_reload",None,"LASER",True,0.8,0.5,0.5,0.2,0.6,False,0.0,None,0,0.9,beam=True,flash=5,two_handed=False),
 "pulserifle": gun("advanced_magazine","advanced_magazine_empty",18.0,7,12,45,17,0.024,4.5,0.0,3,0.015,30.0,45.0,15.0,1.0,False,True,"pulse_fire","pulse_reload",None,"ADVANCED",True,0.35,0.5,0.8,0.3,0.75,False,0.0,None,0,1.0,flash=4),
 "vector": gun("smg_magazine","smg_magazine_empty",10.0,1,25,40,20,0.05,2.0,0.0,1,0.0,17.0,25.0,8.0,0.5,False,True,"vector_fire","vector_reload",None,"BALLISTIC",True,0.75,0.35,0.5,0.3,0.65,False,0.0,None,0,1.0,flash=1),
 "pdw": gun("advanced_magazine","advanced_magazine_empty",10.0,1,40,40,12,0.03,3.5,0.0,1,0.0,18.0,25.0,7.0,1.0,False,True,"pdw_fire","pdw_reload",None,"ADVANCED",True,0.8,0.5,0.4,0.25,0.6,False,0.0,None,0,1.0,flash=4),
 "as50": gun("as50_magazine","as50_magazine_empty",45.0,10,10,80,14,0.0625,6.5,0.0,1,0.0,40.0,60.0,35.0,2.0,True,True,"as50_fire","as50_reload",None,"BALLISTIC",True,0.35,0.125,2.2,0.5,0.9,False,0.0,None,0,1.15,extra_ammo=["as50_magazine_explosive","as50_magazine_incendiary"],flash=1),
}

out.mkdir(parents=True, exist_ok=True)
for name, data in GUNS.items():
    (out / f"{name}.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
print(f"Wrote {len(GUNS)} gun JSON files to {out}")
