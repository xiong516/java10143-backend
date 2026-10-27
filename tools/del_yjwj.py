import os, shutil, sys, time

TARGET = r"E:\xiazai2\yjwj_2026-09-01-21-00"

def main():
    start = time.time()
    files = 0
    freed = 0
    for root, dirs, fs in os.walk(TARGET, topdown=False):
        for f in fs:
            p = os.path.join(root, f)
            try:
                sz = os.path.getsize(p)
                os.remove(p)
                files += 1
                freed += sz
                if files % 2000 == 0:
                    print("removed %d files, %.1f GB freed, %.0fs" % (files, freed/1e9, time.time()-start), flush=True)
            except FileNotFoundError:
                pass
            except Exception as e:
                print("skip %s: %s" % (p, e), flush=True)
        for d in dirs:
            try:
                os.rmdir(os.path.join(root, d))
            except OSError:
                pass
    try:
        shutil.rmtree(TARGET, ignore_errors=True)
    except Exception:
        pass
    print("DONE. files=%d freed=%.1f GB elapsed=%.0fs" % (files, freed/1e9, time.time()-start), flush=True)
    print("DELETED" if not os.path.exists(TARGET) else "STILL-EXISTS", flush=True)

if __name__ == "__main__":
    main()
